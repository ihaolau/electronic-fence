#!/bin/bash -l
pdir=$(cd "`dirname $0`"; cd ../;pwd)
cd "${pdir}"
sed -i 's/charset="GBK"//g' log4j2.xml
java -Xmx512m -cp ".:lib/*" com.huawei.bigdata.FenceSceneProducerMain