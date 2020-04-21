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
    private final ConcurrentMap<String, Host> hosts;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        downloadersExecutor = Executors.newFixedThreadPool(downloaders);
        extractorsExecutor = Executors.newFixedThreadPool(extractors);
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
                    Host host = hosts.compute(hostName, (k, v) -> v == null ? new Host() : v);
                    host.addTask(() -> {
                        try {
                            Document document = downloader.download(u);
                            extractorsExecutor.submit(() -> {
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
        downloadersExecutor.shutdownNow();
        extractorsExecutor.shutdownNow();
    }

    private static class LinkExtractor {
        private final Set<String> passed;
        private BlockingQueue<String> queue;

        LinkExtractor(Set<String> passed) {
            this.passed = passed;
            this.queue = new LinkedBlockingQueue<>();
        }

        void extractLinks(Document document) throws IOException {
            document.extractLinks().stream().filter(passed::add).forEach(queue::add);
        }

        BlockingQueue<String> getQueue() {
            return queue;
        }

        BlockingQueue<String> getAndSetQueue() {
            BlockingQueue<String> queue = getQueue();
            this.queue = new LinkedBlockingQueue<>();
            return queue;
        }
    }

    private class Host {
        private final BlockingQueue<Runnable> queue;
        private final Semaphore semaphore;

        private Host() {
            this.queue = new LinkedBlockingQueue<>();
            this.semaphore = new Semaphore(perHost);
        }

        void addTask(Runnable task) {
            if (semaphore.tryAcquire()) {
                downloadersExecutor.submit(() -> {
                    task.run();
                    runNextTask();
                });
            } else {
                queue.add(task);
            }
        }

        void runNextTask() {
            Runnable task;
            while ((task = queue.poll()) != null) {
                task.run();
            }
            semaphore.release();
        }
    }

    public static void main(String[] args) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Arguments can't be null");
        } else {
            String url;
            int depth = 1;
            int downloaders = 1;
            int extractors = 1;
            int perHost = 10;

            try {
                switch (args.length) {
                    case 5:
                        perHost = Integer.max(1, Integer.parseInt(args[4]));
                    case 4:
                        extractors = Integer.max(extractors, Integer.parseInt(args[3]));
                    case 3:
                        downloaders = Integer.max(downloaders, Integer.parseInt(args[2]));
                        if (args.length != 5) {
                            perHost = Integer.max(perHost, downloaders);
                        }
                    case 2:
                        depth = Integer.max(depth, Integer.parseInt(args[1]));
                    case 1:
                        url = args[0];

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