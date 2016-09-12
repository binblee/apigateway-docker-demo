# Aliyun API Gateway Demo

Sample API Gateway Java Client code, forked from [api-gateway-demo-sign-java](https://github.com/aliyun/api-gateway-demo-sign-java)

# Build

Run gradle build without tests. All tests need to specify APP_KEY, APP_SECRECT and HOST before run test,
otherwise tests will fail.

```
./gradle build -x test
```

# Run

```
export APP_KEY=<placeholder>
export APP_SECRET=<placeholder>
export API_HOST=<placeholder>
java -jar build/libs/apidemoclient-0.0.1-SNAPSHOT.jar
```

