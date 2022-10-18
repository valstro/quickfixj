rm -rf target/data
mvn compile
mvn exec:java -Djava.net.preferIPv4Stack=true -Dexec.mainClass=quickfix.examples.executor.Executor -Dexec.args=executor.cfg
