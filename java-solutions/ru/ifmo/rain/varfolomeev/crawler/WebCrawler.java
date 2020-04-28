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
    private final ExecutorService downloaderExecutor;
    private final ExecutorService extractorExecutor;
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
        downloaderExecutor = Executors.newFixedThreadPool(downloaderCount);
        extractorExecutor = Executors.newFixedThreadPool(extractorCount);
        this.perHost = perHost;
        hosts = new ConcurrentHashMap<>();
    }

    @Override
    public Result download(String url, int depth) {
        Set<String> downloaded = ConcurrentHashMap.newKeySet();
        ConcurrentMap<String, IOException> errors = new ConcurrentHashMap<>();
        Set<String> passed = ConcurrentHashMap.newKeySet();

        BlockingQueue<String> currentQueue = new LinkedBlockingQueue<>();
        LinkExtractor extractor = new LinkExtractor(passed);
        currentQueue.add(url);
        passed.add(url);
        for (int i = 0; i < depth; i++) {
            CountDownLatch latch = new CountDownLatch(currentQueue.size());
            currentQueue.forEach(u -> {
                try {
                    String hostName = URLUtils.getHost(u);
                    HostManager hostManager = hosts.compute(hostName, (k, v) -> v == null ? new HostManager() : v);
                    hostManager.addTask(() -> {
                        try {
                            Document document = downloader.download(u);
                            extractorExecutor.submit(() -> {
                                try {
                                    extractor.extractLinks(document);
                                    downloaded.add(u);
                                } catch (IOException e) {
                                    errors.put(u, e);
                                } finally {
                                    latch.countDown();
                                }
                            });
                        } catch (IOException e) {
                            errors.put(u, e);
                            latch.countDown();
                        }
                    });
                } catch (MalformedURLException e) {
                    errors.put(u, e);
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }
            currentQueue = extractor.getAndSetQueue();
        }

        return new Result(List.copyOf(downloaded), errors);
    }

    @Override
    public void close() {
        downloaderExecutor.shutdownNow();
        extractorExecutor.shutdownNow();
    }

    private static class LinkExtractor {
        private final Set<String> passed;
        private BlockingQueue<String> queue;

        private LinkExtractor(Set<String> passed) {
            this.passed = passed;
            this.queue = new LinkedBlockingQueue<>();
        }

        private void extractLinks(Document document) throws IOException {
            document.extractLinks().parallelStream().filter(passed::add).forEach(queue::add);
        }

        private BlockingQueue<String> getQueue() {
            return queue;
        }

        private BlockingQueue<String> getAndSetQueue() {
            BlockingQueue<String> queue = getQueue();
            this.queue = new LinkedBlockingQueue<>();
            return queue;
        }
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
                downloaderExecutor.submit(() -> {
                    task.run();
                    runNextTask();
                });
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

                        try (Crawler crawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost)) {
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