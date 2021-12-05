### Lab 3 hints for devops :D 


```bash
# Build artifact
$ ./mvnw -f ./proxy-app/pom.xml package -Dmaven.test.skip

# Run tests (includes: integration & unit-tests)
$ ./mvnw -f ./proxy-app/pom.xml test

# Builded artifact path: ./proxy-app/data-warehouse/target/data-warehouse-1.0-SNAPSHOT-jar-with-dependencies.jar

# Logger config file path: ./proxy-app/data-warehouse/src/main/resources/log4j2.xml

# On instance copy artifact and config file in the same folder and run it as shown in the example bellow

# Run app
$ java -jar -Dlog4j.configurationFile="./log4j2.xml" ./data-warehouse-1.0-SNAPSHOT-jar-with-dependencies.jar port=8081


# Good luck :)
```


## Lab2 and 3 PAD (Distributed Proxy Application)

### All modules were created with command:
```
$ maven archetype:generate (maven-archetype-quickstart)
```



Run datawarehouse (on local env):
```
$ ./scripts/runDataWarehouse.sh --port 8080
```




