# Spark RSS example
Test app, that fetching last news from CNN and filters them according to Google Trends

### Building
 - sbt 0.13.18
 - scala 2.11.8
 - jdk 1.8
 
```
sbt assembly
sudo docker build -t rss-test .
```

### Run
```
docker run --rm -it rss-test
```
