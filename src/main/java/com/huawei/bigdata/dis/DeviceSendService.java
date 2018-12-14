package com.huawei.bigdata.dis;

import com.huawei.bigdata.Constants;
import com.huawei.bigdata.filemonitor.DataReaderListener;
import com.huaweicloud.dis.DISClient;
import com.huaweicloud.dis.DISConfig;
import com.huaweicloud.dis.iface.data.request.PutRecordsRequest;
import com.huaweicloud.dis.iface.data.request.PutRecordsRequestEntry;
import com.huaweicloud.dis.iface.data.response.PutRecordsResult;
import com.huaweicloud.dis.iface.data.response.PutRecordsResultEntry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DeviceSendService {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceSendService.class);
    public static boolean isUpdate = true;
    private static String DATE_REGX = "[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{0,3}";
    private static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    DataReaderListener dataReader = new DataReaderListener();
    private AtomicInteger totalCount = new AtomicInteger(0);
    private ScheduledExecutorService service;
    private Map<String, AtomicInteger> indexMap = new ConcurrentHashMap<String, AtomicInteger>();
    private DISConfig disConfig = Constants.DIS_CONFIG;
    private DISClient disClient = new DISClient(disConfig);
    private int max_size = Constants.DEVICE_SEND_THREADPOOL_MAX_SIZE;
    private int deviceNum = Constants.DIS_DEVICE_NUM;
    private int interval = Constants.DIS_SEND_INTERVAL_SEC;

    public void startSend(int num, int intervalSecs) {
        if (service != null) {
            boolean shutdown = service.isShutdown();
            if (!service.isShutdown()) {
                LOG.info("file has alread update,begin re-init sendThread......");
            }
            stopSend();
        }
        service = Executors.newScheduledThreadPool(max_size);
        indexMap.clear();
        for (String key : dataReader.getDeviceIDs()) {
            indexMap.put(key, new AtomicInteger(0));
        }
        LOG.debug("indexMap size is {}.", indexMap.size());
        LOG.debug("start send data");
        int count = 0;
        for (String key : dataReader.getDeviceIDsBySize()) {
            if (count >= num) {
                break;
            }
            LOG.info("start send key {}.", key);
            long current = System.currentTimeMillis();
            long initDelay = current % (1000 * 60) == 0 ? 0 : 60L * 1000 - current % (1000 * 60);
            List<String> wholeData = dataReader.getWholeData(key);
            int countTimes = 0;
            service.scheduleAtFixedRate(new SenderTask(key, wholeData),/*initDelay*/1l, (long) (intervalSecs * 1000), TimeUnit.MILLISECONDS);
            count++;
        }
    }

    public void monitSend(int num, int intervalSecs) {

        while (true) {
            if (isUpdate) {
                startSend(num, intervalSecs);
                try {
                    isUpdate = false;
                    Thread.sleep((long) (5.0 * 1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }
    }


    public void stopSend() {
        service.shutdown();
    }

    class SenderTask implements Runnable {
        private String key;
        private List<String> content;

        public SenderTask(String key, List<String> content) {
            this.key = key;
            this.content = content;
        }

        @Override
        public void run() {
            int size = content.size();
            try {
                AtomicInteger count = indexMap.get(key);
                int index = count.get() % content.size();
                LOG.debug("start send data {}, index is {}", key, index);
                long time = System.currentTimeMillis();
                Date d = new Date(time);
                String date = DATE_FORMAT.format(d);
                // 构造来回路径数据
                String originData = count.get() / content.size() % 2 == 0 ? content.get(index)
                        : content.get(content.size() - 1 - index);
                String data = StringUtils.replacePattern(originData, DATE_REGX, date);
                LOG.debug("data is {}", data);
                LOG.debug("end send data {}, index is {}", key, index);

                if (!sendData(key, data)) {
                    LOG.error("end send data failed!, {}, index is {}", key, index);
                }
                totalCount.getAndIncrement();
                count.getAndIncrement();
            } catch (RuntimeException e) {
                LOG.error("send data {}, error!!!!,{}", key, e.getMessage());
            }
        }

        private boolean sendData(String key, String data) {
            PutRecordsRequest putRecordsRequest = new PutRecordsRequest();
            putRecordsRequest.setStreamName(Constants.INPUT_STREAM_NAME);
            List<PutRecordsRequestEntry> putRecordsRequestEntryList = new ArrayList<>();
            PutRecordsRequestEntry putRecordsRequestEntry = new PutRecordsRequestEntry();
            putRecordsRequestEntry.setData(ByteBuffer.wrap(data.getBytes()));
            putRecordsRequestEntry.setPartitionKey(key);
            putRecordsRequestEntryList.add(putRecordsRequestEntry);
            putRecordsRequest.setRecords(putRecordsRequestEntryList);
            try {
                PutRecordsResult putRecordsResult = disClient.putRecords(putRecordsRequest);
                for (int i = 0; i < putRecordsResult.getRecords().size(); i++) {
                    PutRecordsResultEntry putRecordsResultEntry = putRecordsResult.getRecords().get(i);
                    if (putRecordsResultEntry.getErrorCode() != null) {
                        LOG.error("Success to send [{}], errorMsg={}]",
                                new String(putRecordsRequestEntryList.get(i).getData().array()),
                                (putRecordsResultEntry.getErrorCode() + putRecordsResultEntry.getErrorMessage()));
                    } else {
                        LOG.info("Success to send [{}], offset is {}.",
                                new String(putRecordsRequestEntryList.get(i).getData().array()),
                                putRecordsResultEntry.getSequenceNumber());
                    }
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                return false;
            }
            return true;
        }
    }
}
