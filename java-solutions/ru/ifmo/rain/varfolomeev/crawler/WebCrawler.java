package ru.ifmo.rain.varfolomeev.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService downloadersExecutor;
    private final ExecutorService extractorsExecutor;
    private final int perHost;
    private final ConcurrentMap<String, HostManager> hosts;
    
    /**
     * Creates new WebCrawler instance
     * @param downloader      {@link Downloader} which will be used to download pages
     * @param downloaderCount count of threads to download pages
     * @param extractorCount  count of threads to download pages
     * @param perHost         count of one host pages that can be downloaded in parallel
     */
    public WebCrawler(Downloader downloader, int downloaderCount, int extractorCount, int perHost) {
        this.downloader = downloader;
        downloadersExecutor = Executors.newFixedThreadPool(downloaderCount);
        extractorsExecutor = Executors.newFixedThreadPool(extractorCount);
        this.perHost = perHost;
        hosts = new ConcurrentHashMap<>();
    }

    private void addLink(String url, int depth, Phaser phaser, ConcurrentMap<String, IOException> errors, Set<String> downloaded, Set<String> passed) {
        try {
            String hostName = URLUtils.getHost(url);
            HostManager hostManager = hosts.compute(hostName, (k, v) -> v == null ? new HostManager() : v);

            phaser.register();
            hostManager.addTask(() -> {
                try {
                    Document document = downloader.download(url);
                    phaser.register();
                    extractorsExecutor.submit(() -> extractLinks(url, document, depth - 1, phaser, errors, downloaded, passed));
                } catch (IOException e) {
                    errors.put(url, e);
                } finally {
                    phaser.arrive();
                    hostManager.runNextTask();
                }
            });
        } catch (MalformedURLException e) {
            errors.put(url, e);
        }
    }

    private void extractLinks(String url, Document document, int depth, Phaser phaser,
                              ConcurrentMap<String, IOException> errors, Set<String> downloaded, Set<String> passed) {
        try {
            if (depth > 0) {
                document.extractLinks().stream()
                        .filter(passed::add).forEach(link -> addLink(link, depth, phaser, errors, downloaded, passed));
            }
            downloaded.add(url);
        } catch (IOException e) {
            errors.put(url, e);
        } finally {
            phaser.arrive();
        }
    }

    @Override
    public Result download(String url, int depth) {
        Set<String> downloaded = ConcurrentHashMap.newKeySet();
        ConcurrentMap<String, IOException> errors = new ConcurrentHashMap<>();
        Set<String> passed = ConcurrentHashMap.newKeySet();
        Phaser phaser = new Phaser(1);
        passed.add(url);
        addLink(url, depth, phaser, errors, downloaded, passed);
        phaser.arriveAndAwaitAdvance();
        return new Result(List.copyOf(downloaded), errors);
    }

    @Override
    public void close() {
        downloadersExecutor.shutdownNow();
        extractorsExecutor.shutdownNow();
    }

    private class HostManager {
        private final BlockingQueue<Runnable> queue;
        private final Semaphore semaphore;

        private HostManager() {
            this.queue = new LinkedBlockingQueue<>();
            this.semaphore = new Semaphore(perHost);
        }

        private void addTask(Runnable task) {
            if (semaphore.tryAcquire()) {
                downloadersExecutor.submit(task);
            } else {
                queue.add(task);
            }
        }

        private void runNextTask() {
            Runnable task;
            while ((task = queue.poll()) != null) {
                task.run();
            }
            semaphore.release();
        }
    }

    /**
     * Runs WebCrawler on the certain url
     * Usage: WebCrawler url [depth [downloads [extractors [perHost]]]]
     * @param args array of String representations of WebCrawler arguments
     */
    public static void main(String[] args) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Arguments can't be null");
        } else {
            int depth = 1;
            int downloaders = 1;
            int extractors = 1;
            int perHost = Integer.MAX_VALUE;

            try {
                switch (args.length) {
                    case 5:
                        perHost = Integer.max(1, Integer.parseInt(args[4]));
                    case 4:
                        extractors = Integer.max(extractors, Integer.parseInt(args[3]));
                    case 3:
                        downloaders = Integer.max(downloaders, Integer.parseInt(args[2]));
                    case 2:
                        depth = Integer.max(depth, Integer.parseInt(args[1]));
                    case 1:
                        String url = args[0];

                        try (Crawler crawler = new ru.ifmo.rain.varfolomeev.crawler.WebCrawler(new CachingDownloader(), downloaders, extractors, perHost)) {
                            Result result = crawler.download(url, depth);
                            System.out.println("Successfully downloaded pages:");
                            result.getDownloaded().forEach(System.out::println);
                            System.out.println();
                            System.out.println("Pages downloaded with errors:");
                            result.getErrors().forEach((p, e) -> System.out.println(p + ": " + e.getMessage()));
                        } catch (IOException e) {
                            System.err.println("Unable to create Downloader instance");
                        }
                        break;
                    default:
                        System.err.println("Usage WebCrawler url [depth [downloaders [extractors [perHost]]]]");
                }
            } catch (NumberFormatException e) {
                System.err.println("Can't parse integer: " + e.getMessage());
            }
        }
    }
}