package com.huawei.bigdata;

import com.huaweicloud.dis.DISConfig;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Created on 2018/7/21.
 */
public class Constants {
    public static final DISConfig DIS_CONFIG = DISConfig.buildDefaultConfig();

    public static final String INPUT_STREAM_NAME = DIS_CONFIG.get("input.stream.name", null);
    public static final String OUTPUT_STREAM_NAME = DIS_CONFIG.get("output.stream.name", null);
    public static final String EXTEND_NAME = DIS_CONFIG.get("extend.name", "json");
    public static final int DEVICE_SEND_THREADPOOL_MAX_SIZE = DIS_CONFIG.getInt("device.send.threadpool.max.size", 10);
    public static final int DIS_SEND_INTERVAL_SEC = DIS_CONFIG.getInt("dis.send.interval.sec", 2);
    public static final int DIS_DEVICE_NUM = DIS_CONFIG.getInt("dis.device.num", 20);
    public static final int CONSUMER_SEQUENCE_NUMBER = DIS_CONFIG.getInt("consumer.sequence.number", -1);
    public static final int CONSUMER_PARTITION_ID = DIS_CONFIG.getInt("consumer.partition.id", 0);
    public static final String CONSUMER_DISPLAY_FORMAT = DIS_CONFIG.get("consumer.display.format", "normal");
    public static String DEVICE_DATA_DIR;

    static {
        try {
            DEVICE_DATA_DIR = DIS_CONFIG.get("device.data.dir",
                    new File(URLDecoder.decode(Constants.class.getClassLoader().getResource("").getPath(), "UTF-8")) + File.separator + "data");
        } catch (UnsupportedEncodingException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            DEVICE_DATA_DIR = "data";
        }
    }
}
