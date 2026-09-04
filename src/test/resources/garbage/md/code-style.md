# Code Style

- [Создаение RestTemplate](#RestTemplate)

## RestTemplate

Вместо объявления компонента и при каждом выборе RestTemplate, сразу получать бин RestTemplate

### Было
```java

@Component
public class BscsIntegrationClient {

    public RestTemplate buildRestTemplate() {

        HttpClient client = HttpClients
                .custom()
                .build();

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(client));
        return restTemplate;
    }
}
```

### Рефакторинг A

```java

@Configuration
public class RestTemplateConfig {

    @Bean("metricRestTemplate")
    public RestTemplate restTemplate() {
        HttpClient client = HttpClients.createDefault();
        
        return new RestTemplateBuilder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8")
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
                .build();
    }
}
```

И на прямую вызывать в сервисе:

```java

@Component
public class MetricClient {

    private final RestTemplate restTemplate;

    public MetricClient(@Qualifier("metricRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    // 
    
    @Autowired
    //todo можно добавить @Qualifier("metricRestTemplate") 
    private final RestTemplate metricRestTemplate;
}
```

### Рефакторинг B (в том случае если один и тот же RestTemplate можно использовать для разынх вызвов)

```java
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        HttpClient client = HttpClients.createDefault();

        return new RestTemplateBuilder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8")
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
                .build();
    }
}

@Component
public class MetricClient {

    private final RestTemplate restTemplate;

    public MetricClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
```
---

