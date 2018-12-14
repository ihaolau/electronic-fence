package com.huawei.bigdata;

import com.huawei.bigdata.dis.DeviceSendService;
import com.huawei.bigdata.filemonitor.FileMonitor;

import java.io.IOException;

/**
 * 发送车辆行驶数据到dis，并获取CloudStream处理后的电子围栏数据
 */
public class FenceSceneProducerMain {

    public static void main(String args[]) throws IOException {
        new FileMonitor().startMonitor();
        new DeviceSendService().monitSend(Constants.DIS_DEVICE_NUM, Constants.DIS_SEND_INTERVAL_SEC);
    }
}
