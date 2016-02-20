package org.wikibrain.download;

import com.github.axet.wget.WGet;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadInfo.Part;
import com.github.axet.wget.info.DownloadInfo.Part.States;
import com.github.axet.wget.info.URLInfo;
import com.github.axet.wget.info.ex.DownloadIOCodeError;

import java.io.File;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class to download files from urls.
 * Prints useful logging messages and retries upon failure with exponential backoffs.
 *
 * @author Shilad Sen
 */
public class FileDownloader {
    public static final Logger LOG = LoggerFactory.getLogger(FileDownloader.class);

    private static final int SLEEP_TIME = 500;     // getOneFile takes a break from downloading
    private static final int MAX_ATTEMPT = 10;      // number of attempts before getOneFile gives up downloading the dump
    private static final int DISPLAY_INFO = 10000;  // amount of time between displaying download progress
    private static final int BACKOFF_TIME = 20000;


    private int sleepTime = SLEEP_TIME;
    private int maxAttempts = MAX_ATTEMPT;
    private int displayInfo = DISPLAY_INFO;
    private int backoffTime = BACKOFF_TIME;


    public FileDownloader() {
    }

    public File download(URL url, File file) throws InterruptedException {
        LOG.info("beginning download of " + url + " to " + file);
        for (int i=1; i <= maxAttempts; i++) {
            try {
                AtomicBoolean stop = new AtomicBoolean(false);
                DownloadInfo info = new DownloadInfo(url);
                DownloadMonitor monitor = new DownloadMonitor(info);
                info.extract(stop, monitor);
//                info.enableMultipart();
                file.getParentFile().mkdirs();
                WGet wget = new WGet(info, file);
                wget.download(stop, monitor);
                LOG.info("Download complete: " + file.getAbsolutePath());
                while (!monitor.isFinished()) {
                    Thread.sleep(sleepTime);
                }
                return file;
            } catch (DownloadIOCodeError e) {
                if (i < maxAttempts) {
                    LOG.info("Failed to download " + url +
                            ". Reconnecting in " + (i * backoffTime / 1000) +
                            " seconds (HTTP " + e.getCode() + "-Error " + url + ")");
                    Thread.sleep(backoffTime * i);
                } else {
                    LOG.warn("Failed to download " + file +
                            " (HTTP " + e.getCode() + "-Error " + url + ")");
                }
            }
        }
        return null;
    }

    class DownloadMonitor implements Runnable {
        private final DownloadInfo info;
        private long last;

        DownloadMonitor(DownloadInfo info) {
            this.info = info;
        }

        public boolean isFinished() {
            return info.getState() == URLInfo.States.STOP || info.getState() == URLInfo.States.ERROR || info.getState() == URLInfo.States.DONE;
        }

        @Override
        public void run() {
            switch (info.getState()) {
                case EXTRACTING:
                case EXTRACTING_DONE:
                case DONE:
                    LOG.info("" + info.getState());
                    break;
                case RETRYING:
                    LOG.info(info.getState() + " " + info.getDelay());
                    break;
                case DOWNLOADING:
                    long now = System.currentTimeMillis();
                    if (now > last + displayInfo) {
                        last = now;
                        /*if (info.multipart()) {
                            String parts = "";

                            for (Part p : info.getParts()) {
                                if (p.getState().equals(States.DOWNLOADING)) {
                                    parts += String.format("Part#%d(%.2f) ", p.getNumber(),
                                            p.getCount() / (float) p.getLength());
                                }
                            }

                            System.out.println(String.format("%.2f %s", info.getCount() / (float) info.getLength(),
                                    parts));
                        } else {*/
                            LOG.info(String.format("%s %.1f of %.1f MB (%.1f%%)",
                                            info.getSource(),
                                            info.getCount() / (1024*1024.0),
                                            info.getLength() / (1024*1024.0),
                                            info.getCount() * 100.0 / info.getLength())
                            );
//                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }


    public void setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public void setDisplayInfo(int displayInfo) {
        this.displayInfo = displayInfo;
    }

    public void setBackoffTime(int backoffTime) {
        this.backoffTime = backoffTime;
    }
}
