package com.huawei.bigdata.filemonitor;

import com.huawei.bigdata.Constants;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 文件监控
 *
 * @author
 * @date
 * @file
 */

public class FileMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(FileMonitor.class);
    private String dataDir = Constants.DEVICE_DATA_DIR;
    private int deviceNum = Constants.DIS_DEVICE_NUM;
    private String extendName = Constants.EXTEND_NAME;

    public void startMonitor() {
        File dir = new File(dataDir);
        if (!dir.exists() || !dir.isDirectory()) {
            LOG.error("DataDir is incorrect. {}", dataDir);
            System.out.println("DataDir is incorrect. " + dataDir);
            System.exit(1);
        }

        // 轮询间隔 2 秒
        long interval = TimeUnit.SECONDS.toMillis(2);
        FileAlterationObserver observer = new FileAlterationObserver(dataDir,
                FileFilterUtils.and(FileFilterUtils.fileFileFilter(), FileFilterUtils.suffixFileFilter(extendName)), null);
        DataReaderListener dataReaderListener = new DataReaderListener(dataDir, deviceNum);
        // 先读取现有的数据
        dataReaderListener.getFileAndContent();
        observer.addListener(dataReaderListener);
        FileAlterationMonitor monitor = new FileAlterationMonitor(interval, observer);
        // 开始监控
        try {
            monitor.start();
        } catch (Exception e) {
            LOG.error("start monitor error ,{}", e.toString());
        }
        LOG.info("directory {} is monitored......,interval: {}", dataDir, interval);
    }
}
