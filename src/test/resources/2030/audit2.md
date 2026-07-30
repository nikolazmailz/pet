<!-- TOC -->

* [Общее описание работы аудита и библиотеки](#общее-описание-работы-аудита-и-библиотеки)
* [Руководство по интеграции через библиотеку tsau-audit-lib](#руководство-по-интеграции-через-библиотеку-tsau-audit-lib)
    * [Внедрение библиотеки tsau-audit-lib](#внедрение-библиотеки-tsau-audit-lib)
    * [Использование AuditTemplate (ServletAuditTemplate или ReactiveAuditTemplate)](#использование-audittemplate-servletaudittemplate-или-reactiveaudittemplate)
    * [Настройка заполнения сообщения аудита](#настройка-заполнения-сообщения-аудита)
      * [Статичные значения полей](#статичные-значения-полей)
      * [Значения полей заполняемые в процессе выполнения метода](#значения-полей-заполняемые-в-процессе-выполнения-метода-resolver)
      * [Настройка резолверов для события](#настройка-резолверов-для-события)
    * [Дополнительные резолверы](#дополнительные-резолверы)
      * [Заполнение информации об ошибке](#заполнение-информации-об-ошибке)
      * [Заполнение параметров из заголовка X-Forwarded-For](#заполнение-параметров-из-заголовка-x-forwarded-for)
      * [Интеграция с ролевой моделью (заполнение staff_roleId, staff_id). Ошибки авторизации]()
    * [Настройка резервной кафки аудита (StandIn)](#настройка-резервной-кафки-аудита-standin)
    * [Kafka slider](#slider)
    * [Настройка поведения библиотеки при недоступности СС "Аудит"](#настройка-поведения-библиотеки-при-недоступности-сс-аудит)
        * [Правила управления блокировкой](#правила-управления-блокировкой)
        * [Callback интерфейс для неотправленных событий](#callback-интерфейс-для-неотправленных-событий)
        * [Модуль хранения неотправленных событий в памяти](#модуль-хранения-неотправленных-событий-в-памяти)
    * [Выбор модулей аудита для подключения](#выбор-модулей-аудита-для-подключения)
        * [Модули для извлечения данных события из контекста](#модули-для-извлечения-данных-события-из-контекста)
        * [Модуль для подключения метрик](#модуль-для-подключения-метрик)
    * [Проверка лога в вашей системе](#проверка-лога-в-вашей-системе)
* [Дополнительные возможности и описания](#дополнительные-возможности-и-описания)
    * [Отправка событий в кафку](#отправка-событий-в-кафку)
    * [Настройка TLS соединения в tsau-audit-lib](#настройка-tls-соединения-в-tsau-audit-lib)
    * [Настройка параметров событий ИБ для передачи в ОКС](#настройка-параметров-событий-иб-для-передачи-в-окс)
* [Включение health-check кафки](#включение-health-check-кафки)
  * [Параметры](#параметры)
  * [Параметры типов событий](#параметры-типов-событий)
  * [Параметры кафки](#параметры-кафки)
* [Известные проблемы](#известные-проблемы)

Общее описание работы аудита и библиотеки<a name="общее-описание-работы-аудита-и-библиотеки"></a>
==========================================================================================

Поставщик событий (приложение подключаемое к аудиту, ваше приложение) отправляет сообщения в кафку аудита, 
СС Аудит читает из кафки сообщения и помещает их в хранилище.

Сообщение в кафку отправляются в формате avro.
Avro схемы хранятся в кафке Аудита в топике audit-v2-schema. У СС Аудит есть специальный микросервис который наполняет топик audit-v2-schema актуальными схемами.
Топик audit-v2-schema компактефицированный, и у каждого сообщения есть 2 заголовка:<br>
schema_type - тип схемы, <br>
schema_version - версия схемы.<br>
Сообщения аудита должны формироваться по последней версии схемы.
При старте библиотека Аудита читает топик audit-v2-schema и сохраняет в памяти актуальные авро схемы.

При описании событий аудита в приложении указывается схема по которой будет формироваться событие
~~~yaml
audit-events:
  auditEventCodeList:
    - event-code: "EXAMPLE_EVENT_CODE"
      schema: "auth_internal"
~~~
В пример схема auth_internal. 
Библиотека перед отправкой заполняет поля с помощью набора резолверов (см. ниже), затем переводит их в формат avro и отправляет в кафку.


Руководство по интеграции через библиотеку tsau-audit-lib<a name="руководство-по-интеграции-через-библиотеку-tsau-audit-lib"></a>
==========================================================================================

Внедрение библиотеки tsau-audit-lib<a name="внедрение-библиотеки-tsau-audit-lib"></a>
----------------------------------------------------------

**1. Необходимо добавить зависимости**

~~~xml
        <!-- TSAU -->
        <!-- Подключить BOM с зависимостями -->
        <dependency>
            <groupId>ru.vtb.omni</groupId>
            <artifactId>omni-dependencies</artifactId>
            <version>${omni-dependencies.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
            
        <!-- модуль хранения событий аудита в оперативной памяти -->
        <dependency>
            <groupId>ru.vtb.omni</groupId>
            <artifactId>tsau-audit-lib-in-memory-storage</artifactId>
        </dependency>
        <!-- модуль отправки событий аудита непосредственно в кафку -->
        <dependency>
            <groupId>ru.vtb.omni</groupId>
            <artifactId>tsau-audit-lib-kafka-sender</artifactId>
        </dependency>
        <!-- модуль контекста, который используется для получения инициатора запроса, в данном случае servlet context -->
        <dependency>
            <groupId>ru.vtb.omni</groupId>
            <artifactId>tsau-audit-lib-servlet-context</artifactId>
        </dependency>
~~~

Для интеграции аудита в реактивных приложениях на базе Spring WebFlux необходимо использовать `tsau-audit-lib-reactive-context` вместо `tsau-audit-lib-servlet-context`:
~~~xml
        <!-- модуль контекста, который используется для получения инициатора запроса, в данном случае reactive context -->
        <dependency>
            <groupId>ru.vtb.omni</groupId>
            <artifactId>tsau-audit-lib-reactive-context</artifactId>
        </dependency>
~~~

**2. Зарегистрировать события аудита.**

Регистрировать события аудита необходимо в application.yml

<strong>event_code</strong> - Код события<br/>
<strong>schema</strong> - Схема события<br/>

Пример реализации:

~~~yaml
audit-events:
  auditEventCodeList:
    - event-code: "EXAMPLE_EVENT_CODE"
      schema: "auth_internal"
~~~

**3. Аннотировать аудируемые методы**

Пример использования аннотации `@Audit`:

~~~java
    @Audit(value = "EXAMPLE_EVENT_CODE")
    @GetMapping("/hello")
    public String hello() {
        return "Hello!";
    }
~~~

Для методов, возвращающих Mono\<T\> или Flux\<T\>, используются аннотации `@AuditMono` или `@AuditFlux` соответственно из модуля `tsau-audit-lib-reactive-context`:

~~~java
    @AuditMono(value = "EXAMPLE_EVENT_CODE_MONO")
    @GetMapping("/mono/hello")
    public Mono<String> hello() {
        return Mono.just("Hello!");
    }

    @AuditFlux(value = "EXAMPLE_EVENT_CODE_FLUX")
    @GetMapping("/flux/hello")
    public Flux<String> streamMessages() {
        return Flux.just("msg1", "msg2", "msg3");
    }
~~~

**4. Задать основные параметры в application.yaml**

Кафка (также есть возможность использовать SSL bundle):
~~~yaml
audit:
  kafka:
    bootstrap-servers: localhost:9092
    security:
      protocol: SSL
    ssl:
      key-store-location: 'file:/etc/certificate/kafka-keystore.jks'
      key-store-type: 'JKS'
      key-store-password: ***
      trust-store-location: 'file:/etc/certificate/kafka-truststore.jks'
      trust-store-type: 'JKS'
      trust-store-password: ***
      key-password: ***
~~~

Код системы и наименование приложения (название микросервиса):

~~~yaml
ms:
  properties:
    infoSystemCode: INFO_SYSTEM_CODE_ALE
    appName: Example
    infoSystemId: "1244253"
~~~

infoSystemCode - код системы<br>
appName - наименование приложения (если не заполнен, будет взят spring.application.name; если ничего не заполнено, будет ошибка при старте)

**5. Настроить резолверы (см. ниже)**


Использование AuditTemplate (ServletAuditTemplate или ReactiveAuditTemplate)<a name="использование-audittemplate-servletaudittemplate-или-reactiveaudittemplate"></a>
----------------------------------------------------------------------------

Если вы по каким-то причинам не хотите использовать аспекты, вы можете воспользоваться AuditTemplate.

**1. Необходимо добавить зависимости**

Для использования `ServletAuditTemplate`:
~~~xml
        <!-- TSAU -->
        <!-- модуль хранения событий аудита в оперативной памяти -->
        <dependency>
            <groupId>ru.vtb.omni</groupId>
            <artifactId>tsau-audit-lib-in-memory-storage</artifactId>
        </dependency>
        <!-- модуль отправки событий аудита непосредственно в кафку -->
        <dependency>
            <groupId>ru.vtb.omni</groupId>
            <artifactId>tsau-audit-lib-kafka-sender</artifactId>
        </dependency>
        <!-- модуль контекста, который используется для получения инициатора запроса, в данном случае audit template -->
        <dependency>
            <groupId>ru.vtb.omni</groupId>
            <artifactId>tsau-audit-lib-template-context</artifactId>
        </dependency>
~~~

Для использования `ReactiveAuditTemplate` подключается `tsau-audit-lib-reactive-context` вместо `tsau-audit-lib-template-context`:

~~~xml
        <!-- модуль контекста, который используется для получения инициатора запроса, в данном случае reactive context -->
        <dependency>
            <groupId>ru.vtb.omni</groupId>
            <artifactId>tsau-audit-lib-reactive-context</artifactId>
        </dependency>
~~~

**2. Зарегистрировать события аудита. *(как описано выше)***

**3. Включить аудит *(как описано выше)***

**4. Задать основные параметры в application.yaml *(как описано выше)***

**5. Пример использования:**

При использовании ServletAuditTemplate:

~~~java
public class AuditExampleReceiver {

    public void foo() {
        String result = servletAuditTemplate.execute(
                "EXAMPLE_EVENT_CODE",                              // eventCode
                Collections.emptyMap(),                            // contextAuditEvent
                Collections.emptyMap(),                            // methodParams
                () -> "OK. See message in kafka audit-v2 topic."   // Supplier<T> или Runnable
        );
    }
}
~~~

При использовании ReactiveAuditTemplate:

~~~java
public class AuditExampleReceiver {

    public void fooMono() {
        Mono<String> result = reactiveAuditTemplate.execute(
                "EXAMPLE_EVENT_CODE_MONO",                // eventCode
                Collections.emptyMap(),                   // contextAuditEvent
                Collections.emptyMap(),                   // methodParams
                ReturnTypeInfo.AUDIT_MONO_TYPE,           // ReturnTypeInfo
                () -> Mono.just("Hello!")                 // Supplier<Mono<T>> или Runnable
        );
    }
    
    public void fooFlux() {
        Flux<String> result = reactiveAuditTemplate.execute(
                "EXAMPLE_EVENT_CODE_FLUX",                // eventCode
                Collections.emptyMap(),                   // contextAuditEvent
                Collections.emptyMap(),                   // methodParams
                ReturnTypeInfo.AUDIT_FLUX_TYPE,           // ReturnTypeInfo
                () -> Flux.just("msg1", "msg2", "msg3")   // Supplier<Flux<T>> или Runnable
        );
    }
}
~~~

**6. Настроить резолверы (см. ниже)**

**Пример использования с передачей полного сообщения аудита.**

У auditTemplate есть возможность передать полностью сформированное сообщение аудита.
В этом случае сообщение передаваемое в auditTemplate будет отправлено ровно таким каким его сформировали, 
только добавится логика START-SUCCESS (будут заполнены поля timestamp, clazz). 
Поле id будет заполняться только для сообщений SUCCESS и FAILURE, для START его обязательно заполнять.
Все остальные обязательные поля необходимо будет заполнить самостоятельно. 
При этом резолверы не будут работать.

Пример использования с ServletAuditTemplate (для ReactiveAuditTemplate аналогично):

~~~java
public class AuditExampleReceiver {
    public String exampleAuditTemplateNotModified(Map<String, Object> auditEvent) {
        return auditTemplate.execute(
                auditEvent,
                () -> "Some method"
        );
    }
}
~~~

**Пример использования с передачей сообщения аудита и заполнения резолверами**

В этом случае сообщение передаваемое в auditTemplate будет дозаполнено резолверами.
При этом переданные предзаполненные значения будут иметь более высокий приоритет.

Пример использования с ServletAuditTemplate (для ReactiveAuditTemplate аналогично):

~~~java
public class AuditExampleReceiver {
    public String exampleAuditTemplateWithResolver() {        
        Map<String, Object> contextAuditEvent = new HashMap<>();
        contextAuditEvent.put("oper_description", "Операция из AuditTemplate");
        
        return auditTemplate.execute(
                "EXAMPLE_EVENT_CODE",
                contextAuditEvent,
                Collections.emptyMap(),
                () -> "Some method"
        );
    }
}
~~~

Стоит обратить внимание, что если нужны резолверы, которые используют web-контекст:<br>
для `ServletAuditTemplate` дополнительно подключается модуль tsau-audit-lib-servlet-context;<br>
для `ReactiveAuditTemplate` они подключены по умолчанию, и если нет web-контекста, их можно отключить при настройке события (см. ниже).

Настройка заполнения сообщения аудита<a name="настройка-заполнения-сообщения-аудита"></a>
------------------------------------------

Статичные значения полей<a name="статичные-значения-полей"></a>
------------------------------------------

В **application.yml** для каждого кода события можно указать значения полей, которые не меняются.
Пример:
```yaml
audit-events:
  auditEventCodeList:
    - event-code: "EXAMPLE_EVENT_CODE"
      schema: "auth_internal"
      audit-event-general:
        oper_name: "Просмотр отчета"
        object_name: "Отчет"
      audit-event-start:
        oper_description: "Начало операции просмотра отчета"
      audit-event-success:
        oper_description: "Окончание операции просмотра отчета"
      audit-event-failure:
        oper_description: "Ошибка при операции просмотра отчета"

```
**audit-event-general** - поля, которые будут заполняться для всех классов сообщений события. </br>
**audit-event-start** - поля, которые будут заполняться для сообщений класса START. </br>
**audit-event-success** - поля, которые будут заполняться для сообщений класса SUCCESS. </br>
**audit-event-failure** - поля, которые будут заполняться для сообщений класса FAILURE. </br>


Значения полей заполняемые в процессе выполнения метода (Resolver)<a name="значения-полей-заполняемые-в-процессе-выполнения-метода-resolver"></a>
------------------------------------------

Библиотека аудита формирует сообщение для отправки в кафку с помощью резолверов.</br>
Резолверы принимают на вход тип события и параметры аудируемого метода, и на основе их выдают значение конкретного поля или сразу несколько полей.
При этом если в схеме события нет поля, которое формирует резолвер, это поле не будет учитываться и резолвер не будет выполняться.

Существует 2 вида резолверов:

**AuditFieldResolver**

Интерфейс AuditFieldResolver необходимо для заполнения одного конкретного поля.
Это основной тип резолверов.

```java
/**
 * Определеяет значения поля для формируемого сообщения аудита
 */
public interface AuditFieldResolver {

    /**
     * Возвращает значение поля
     *
     * @param eventCode         Код события формируемого сообщения
     * @param audLibEventClass  Класс сообщения
     * @param auditMethodParams Параметры вызова аудируемого метода
     * @param currentFields     Текущие заполненные поля
     * @return Значения поля для формируемого сообщения
     */
    Object get(String eventCode, AudLibEventClass audLibEventClass,
               AuditMethodParams auditMethodParams, Map<String, Object> currentFields, Map<String, Object> contextOperation);

    /**
     * Возвращает наименование поля для которого формируется значение
     *
     * @return Наименование поля
     */
    String getFieldName();
}
```

Пример реализации интерфейса:
```java
@RequiredArgsConstructor
public class InfoSystemCodeTsauResolver implements AuditFieldResolver {

    private final AuditMsProperties auditMsProperties;

    @Override
    public Object get(String eventCode, AudLibEventClass audLibEventClass, AuditMethodParams auditMethodParams, Map<String, Object> currentFields, Map<String, Object> contextOperation) {
        return auditMsProperties.getInfoSystemCode();
    }

    @Override
    public String getFieldName() {
        return "event_infoSystemCode";
    }
}
```

Данный пример заполняет поле event_infoSystemCode из yml конфигурации

**AuditListFieldsResolver**

Интерфейс AuditListFieldsResolver в отличие от AuditFieldResolver может заполнить сразу несколько полей. 
AuditListFieldsResolver возвращает Map<String, Object> где ключ это наименование поле из схемы события, а значение - значение этого поля.

```java
public interface AuditListFieldsResolver {

    /**
     * Возвращает значение списка полей
     *
     * @param eventCode         Код события формируемого сообщения
     * @param audLibEventClass  Класс сообщения
     * @param auditMethodParams Параметры вызова аудируемого метода
     * @param currentFields     Текущие заполненные поля
     * @return Мапа где ключ - имя поля в схеме, значение - значения поля
     */
    Map<String, Object> getFields(String eventCode, AudLibEventClass audLibEventClass,
                                  AuditMethodParams auditMethodParams, Map<String, Object> currentFields, Map<String, Object> contextOperation);
}
```

Пример:
```java
@RequiredArgsConstructor
public class MsPropertiesResolverList implements AuditListFieldsResolver {

    private final AuditMsProperties auditMsProperties;

    @Override
    public Map<String, Object> getFields(String eventCode, AudLibEventClass audLibEventClass, AuditMethodParams auditMethodParams, Map<String, Object> currentFields, Map<String, Object> contextOperation) {
        Map<String, Object> result = new HashMap<>();
        result.put("event_infoSystemCode", auditMsProperties.getInfoSystemCode());
        result.put("event_infoSystemId", auditMsProperties.getInfoSystemId());
        return result;
    }
}
```
Данный пример заполняет поле event_infoSystemCode и event_infoSystemId из yml конфигурации

### Параметры вызова аудируемого метода (auditMethodParams) Аннотация @AuditParam

AuditMethodParams - это ДТО для передачи информации о аудируемом методе, который передается в резолверы.
Он состоит из полей: <br>
**Map<String, Object> params** - параметры аудируемого метода, помеченные аннотацией @AuditParam<br>
**Object returnValue** - результат выполнения метода<br>
**Throwable error** - ошибка возникшая при выполнении метода (используется в ExceptionListResolver)

Без аннотации @AuditParam параметры метода не передадутся в AuditMethodParams.
У аннотации @AuditParam есть один параметр - 
**value** значением этого параметра будет использовано в качестве ключа в AuditMethodParams.params

### Резолверы библиотеки аудита

В библиотеки аудита уже реализовны ряд резолверов

|Наименование поля     | Наименование класса резолвера | Порядок | Описание логики формирования значения поля |
|----------------------|-------------------------------|---------|--------------------------------------------|
|event_infoSystemCode  | InfoSystemCodeTsauResolver    | 1       | Получает значение из **application.yml** проперти **ms.properties.info-system-code** | 
|event_infoSystemId    | InfoSystemIdTsauResolver      | 2       | Получает значение из **application.yml** проперти **ms.properties.info-system-id** | 
|event_userSessionId   | SessionIdTsauResolver         | 16      | Получает из токена, поле в токене **ctxi** | 
|event_techCodes       | TechSectionTsauResolver       | 3       | Из параметра аудируемого метода **event_techCodes**. Если не заполненно, то из **application.yml** проперти **audit.event.tech-section-codes**| 
|context_namespace     | NamespaceTsauResolver         | 4       | Из переменной окружения AUDITOMNI_POD_NAMESPACE. В **application.yml** можно изменить из какой переменной окружения получать. Проперти **audit.deployment-context.pod-namespace-environment** | 
|context_podName       | PodNameTsauResolver           | 5       | Из переменной окружения AUDITOMNI_POD_NAME. В **application.yml** можно изменить из какой переменной окружения получать. Проперти **audit.deployment-context.pod-name-environment** | 
|context_nearbyNodeIp  | NearbyNodeIpTsauResolver      | 14      | Из контекста HTTP запроса (ServletRequest::getRemoteAddr) | 
|context_recipientIp   | RecipientIpTsauResolver       | 15      | Из контекста HTTP запроса (ServletRequest::getLocalAddr) | 
|context_method        | MethodTsauResolver            | 13      | Из контекста HTTP запроса (HttpServletRequest::getMethod) | 
|context_url           | UrlTsauResolver               | 17      | Из контекста HTTP запроса. Объединение полей HttpServletRequest::getRequestURI + HttpServletRequest::getQueryString | 
|initiator_channel     | ChannelTokenTsauResolver      | 10      | Получает из токена, поле в токене **channel** | 
|initiator_channel     | ChannelPropertiesTsauResolver | 8       | Получает значение из **application.yml** проперти **audit.default-resolver.channel** | 
|initiator_sub         | LoginTokenTsauResolver        | 11      | Получает из токена, поле в токене **sub** | 
|initiator_sub         | LoginPropertiesTsauResolver   | 9       | Получает значение из **application.yml** проперти **audit.default-resolver.sub** | 
|initiator_sourceIp    | IpAddressTsauResolver         | 12      | Из стандартного заголовка X-Forwarded-For HTTP запроса (требуется указать количество доверенных прокси через параметр). Если в заголовке нет, то из контекста HTTP запроса (ServletRequest::getRemoteAddr) | 
|initiator_realm       | RealmTokenTsauResolver        | 18      | Получает из токена, поле в токене **realm** | 
|initiator_clientAppId | ClientAppIdTokenTsauResolver  | 19      | Получает из токена, поле в токене **client_id** | 
|initiator_device_fingerPrint | DeviceFingerPrintTsauResolver | 20      | Из заголовка **X-Device-Fingerprint** HTTP запроса. | 
|oper_resultStatus     | OperResultStatusTsauResolver  | 21      | Если операция завершилась с ошибкой безнесс логики (BusinessLogicAuditException) возвращает FAILURE, иначе SUCCESS

Настройка резолверов для события<a name="настройка-резолверов-для-события"></a>
-------------------------------------------------------------

В первую очередь берутся статичные поля из **application.yml** </br>
Потом применяются резолверы с интерфейсом **AuditListFieldsResolver** </br>
В конце применяются резолверы с интерфейсом **AuditFieldResolver** </br>

По умолчанию, если нет никаких настроек резолверов для типа события, применяются все резолверы которые нашел Spring по интерфейсу.
Для каждого типа события можно настроить в **application.yml** перечень резолверов которые будут применяться к конкретному типу событий

```yaml
audit-events:
  auditEventCodeList:
    - event-code: "EXAMPLE_EVENT_CODE"
      resolvers: LoginTokenTsauResolver, ChannelTokenTsauResolver
      list-resolvers: MsPropertiesResolverList
      exclude-resolvers: LoginPropertiesTsauResolver, ChannelPropertiesTsauResolver
      schema: "auth_internal"
```

**resolvers** - наименования классов резолверов интерфейса **AuditFieldResolver**, которые будут применяться к типу события. При этом никакие другие резолверы не будут выполняться;</br>
**list-resolvers**  - наименования классов резолверов интерфейса **AuditListFieldsResolver**, которые будут применяться к типу события;</br>
**exclude-resolvers**  - наименования классов резолверов интерфейса **AuditFieldResolver** и **AuditListFieldsResolver**, которые не будут применятся к типу события. 
Если раздел **resolvers** не будет заполнен, с помощью этой настройки можно включить все ресолверы за исключением не нужных.</br>

У типа события может быть на одно поле только один **AuditFieldResolver**. 
Если например для поля **initiator_sub** есть резолверы **LoginTokenTsauResolver** и **LoginPropertiesTsauResolver** применится только один, тот у которого порядок выше.
В нашем случае применится **LoginTokenTsauResolver** так как у него порядок 10 а у **LoginPropertiesTsauResolver** 8.
При написании своих резолверов порядок задается с помощью аннотации @Order

Для **AuditListFieldsResolver** аннотация @Order влияет на то, в каком порядке резолверы будут выполняться. Чем выше ордер, тем больший приоритет. 
Резолверы с высоким порядком будут перетирать значения, которые могли заполнить резолверы с низким @Order

### Группы резолверов

Для простоты конфигурации можно настроить группы резолверов

```yaml
audit-group-resolver:
  general: InfoSystemCodeTsauResolver, InfoSystemIdTsauResolver, ...
  web: LoginTokenTsauResolver, ChannelTokenTsauResolver, ...
  process: LoginPropertiesTsauResolver, ChannelPropertiesTsauResolver
```

тогда настройка типа событий будет выглядеть так

```yaml
audit-events:
  auditEventCodeList:
    - event-code: "EXAMPLE_EVENT_CODE"
      group-resolvers: general, web
      schema: "auth_internal"
```

При этом будут применятся только резолверы из групп, за исключением тех которые указаны в **exclude-resolvers**</br>
**AuditListFieldsResolver** включать в группы нельзя, так как они по сути тоже группы, просто прописанные в коде
В библиотеке уже есть преднастроенные группы которые можно использовать без дополнительной конфигурации

|Наименование группы | Наименование бинов резолверов в группе                                                                                                                                                                                                                                                                             |
|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|general             | InfoSystemCodeTsauResolver, InfoSystemIdTsauResolver, NamespaceTsauResolver, PodNameTsauResolver, SpanTsauResolver, TechSectionTsauResolver, TraceTsauResolver, OperResultStatusTsauResolver                                                                                                                                                   | 
|web                 | ChannelTokenTsauResolver, LoginTokenTsauResolver, IpAddressTsauResolver, ClientAppIdTokenTsauResolver, MethodTsauResolver, NearbyNodeIpTsauResolver, RealmTokenTsauResolver, RecipientIpTsauResolver, SessionIdTsauResolver, UrlTsauResolver, DeviceFingerPrintTsauResolver, TsrmRoleResolver, TsrmStaffIdResolver | 
|process             | ChannelPropertiesTsauResolver, LoginPropertiesTsauResolver                                                                                                                                                                                                                                                         | 

Дополнительные резолверы<a name="дополнительные-резолверы"></a>
-------------------------------------------------------------

Заполнение информации об ошибке<a name="заполнение-информации-об-ошибке"></a>
-------------------------------------------------------------

Для заполнения ошибки при выполнении метода в поле **oper_description** библиотеке аудита реализован резолвер **ExceptionListResolver** с порядком 100.
Для его включение необходимо поставить проперти **audit.fill-error** в true.
Данный резолвер дополняет значение **oper_description** сообщением об ошибке. 
Полная реализация:

```java
public class ExceptionListResolver implements AuditListFieldsResolver {
    @Override
    public Map<String, Object> getFields(String eventCode, AudLibEventClass audLibEventClass, AuditMethodParams auditMethodParams, Map<String, Object> currentFields, Map<String, Object> contextOperation) {
        if (auditMethodParams.getError() != null) {
            Map<String, Object> result = new HashMap<>();
            if (currentFields.containsKey(OPER_DESCRIPTION_FIELD_NAME)) {
                if (currentFields.get(OPER_DESCRIPTION_FIELD_NAME).toString().startsWith("Описание ошибки"))
                {
                    return null;
                }
                result.put(OPER_DESCRIPTION_FIELD_NAME, String.format("%s %s", currentFields.get(OPER_DESCRIPTION_FIELD_NAME), getErrorMessage(auditMethodParams)));
            } else {
                result.put(OPER_DESCRIPTION_FIELD_NAME, getErrorMessage(auditMethodParams));
            }
            return result;
        }
        return null;
    }

    private String getErrorMessage(AuditMethodParams auditMethodParams) {
        return String.format("Описание ошибки: %s", auditMethodParams.getError().getMessage());
    }
}
```

Заполнение параметров из заголовка X-Forwarded-For<a name="заполнение-параметров-из-заголовка-x-forwarded-for"></a>
-------------------------------------------------------------

Для заполнения параметров из массива модифицированного заголовка **X-Forwarded-For** реализован резолвер **XForwardedForListResolver** с порядком 101.</br>
Данный резолвер заполняет значения из этого заголовка, которые перечисляются следующим образом:

Первое значение 
- **initiator_sourceIp** </br>

Второе значение:</br>
- либо **initiator_sourcePort** </br>
- либо **context_nearbyNodeIp** </br>

Третье значение: **context_nearbyNodeIp** (при условии, что вторым значением был порт).</br>

Для его включения необходимо поставить параметр **audit.ip-port-custom-header-resolver** со значением true.


Заполнение oper_resultStatus, обработка логических ошибок по ШП.116
-------------------------------------------------------------

Согласно ШП116 логические ошибки необходимо отправлять с классом SUCCESS, но заполненным полем
oper_resultStatus: "FAILURE". Для того чтобы обозначить ваши логические ошибки необходимо использовать (или унаследоваться)
BusinessLogicAuditException. Если библиотека увидит что метод выкинул exception BusinessLogicAuditException, то сообщение заудируется с классом SUCCESS,
а резолвер OperResultStatusTsauResolver проставит oper_resultStatus

Интеграция с ролевой моделью (заполнение staff_roleId, staff_id). Ошибки авторизации
-------------------------------------------------------------

Для интеграции с Ролевой моделью написана специальная библиотека *omni-access-control-lib-audit-v2* для ее включения необходимо добавить зависимость (при условии что уже подключена библиотека РМ)

~~~xml
        <dependency>
            <groupId>ru.vtb.omni</groupId>
            <artifactId>omni-access-control-lib-audit-v2-context</artifactId>
        </dependency>
~~~

В ней реализованы и включены по умолчанию резолверы

| Наименование поля | Наименование класса резолвера | Порядок | Описание логики формирования значения поля                                     |
|-------------------|-------------------------------|---------|--------------------------------------------------------------------------------|
| staff_roleId      | TsrmRoleResolver              | 2       | Получение роли с которой авторизовался пользователь из контекста бибилотеки РМ |
| staff_id          | TsrmStaffIdResolver           | 3       | SubjectCode пользователя из контекста библиотеки РМ                            |

Также в этом модуле реализован коллбек РМ на не успешную авторизацию и аутентификацию. В аудите API 2.0 неуспешная авторизация и аутентификация аудируется под тем event_code что и аудируемый метод, при этом заполняются поля oper_resultStatus, oper_resultReason, oper_description.
Аудит неуспешной авторизации включен всегда, для аудита неуспешной аутентификации необходимо проставить флаг

```yaml
aclib:
  auth-audit-enabled: true
```

Настройка резервной кафки аудита (StandIn)<a name="настройка-резервной-кафки-аудита-standin"></a>
------------------------------------------

Есть возможность настроить резервную кафку в которую будут отправляться сообщения при недоступности основной кафки.

**Алгоритм работы**: библиотека пытается отправить сообщение в основную кафку, если это не удается, сообщение
отправляется в резервную, если не удается и в резервную - выбрасывается исключение.  
При этом, после заданного числа (по умолчанию - 2) неудачных попыток отправки в основную кафку происходит переключение и
резервная кафка становится основной на заданное время (по умолчанию - 5 мин), а основная - резервной.

**Параметры управления резервной кафкой:**

~~~yaml
# Включение резервной кафки - по умолчанию выключено
audit.standin-kafka.enabled: true

# Время работы в standin после переключения (default - 5 min)
audit.standin-period-min: 5

# Число неудачных попыток для переключения основного продюсера на standin
audit.standin-try-count-to-switch: 2

# Настройки резервной кафки
audit.standin-kafka:
...
~~~

**Пример параметров:**

~~~yaml
audit:
  ...
  standin-period-min: 5
  standin-try-count-to-switch: 2
  standin-kafka:
    enabled: true
    bootstrap-servers: "kafka-service:9092"
    security:
      protocol: SSL
    ssl:
      key-store-location: 'file:/etc/certificate/kafka-keystore.jks'
      key-store-type: 'JKS'
      key-store-password: ***
      trust-store-location: 'file:/etc/certificate/kafka-truststore.jks'
      trust-store-type: 'JKS'
      trust-store-password: ***
      key-password: ***
~~~

Kafka slider<a name="slider"></a>
-----------------------------------------------------------

В случае если одна из партиций кафки станет не доступной, то стандартное поведение SpringKafka не отправит сообщение а
выкинет Exception. Slider сделан для того чтобы в случае если партиция не доступна, а другие партиции работают,
сообщение отправлялось в рабочую партицию. И только в том случае, если нет рабочих партиций происходило перелючение на
StandIn кафку.

Для отключения слайдера, чтобы использовался SpringKafka клиент, необходимо прописать:

~~~yaml
audit:
  kafka:
    properties:
      slider.enable: "false"
  standin-kafka:
    properties:
      slider.enable: "false"
~~~

При этом время для переключения на стендин будет увеличено так как добавляется таймаут поиска живых партиций и запроса
поиска партиций

~~~yaml
audit:
  kafka:
    producer:
      properties:
        max.block.ms: 10000
        default.api.timeout.ms: 10000
        request.timeout.ms: 10000
    slider.wait.ms: 5000
~~~

max.block.ms - время которое будет пытаться отправиться сообщение в партицию slider.wait.ms - время поиска живых
партиций default.api.timeout.ms и request.timeout.ms - время для получения перечня всех партиций


Настройка поведения библиотеки при недоступности СС "Аудит"<a name="настройка-поведения-библиотеки-при-недоступности-сс-аудит"></a>
-----------------------------------------------------------

По умолчанию если не удалось отправить событие аудита в кафку, то аудируемый метод продолжит работать, при этом событие
будет утеряно. Если события обязательно и не должно теряться, был разработан модуль audit-lib-blocking-context. Данный
модуль подключается добавлением зависимости

~~~xml
<dependency>
    <groupId>ru.vtb.omni</groupId>
    <artifactId>tsau-audit-lib-blocking-context</artifactId>
</dependency>
~~~

После подключения модуля, появляется возможность сделать событие аудита блокируемым, для этого application.yaml
необходимо прописать:

~~~yaml
audit-events:
  block-settings:
    VIEW_REPORT: true
~~~

Так события с типом VIEW_REPORT будут завершаться исключением если не удалось отправить сообщение в кафку, все остальные
события не будут блокировать работу аудируемого метода.

Так же изменить поведение можно через динамические настройки. Ключ: audit-events.block-settings.VIEW_REPORT , значение "
true".

В случае когда blocking: false или не указан, при первом обнаружении, что кафка не доступна, тогда включается таймер, во
время действия таймера, все операции у которых blocking: false, будут пропускаться. Время в течении которого операции
будут пропускаться до повторного обращения в кафку можно задать через параметр **audit.blocking.blocking-skip-time** (по
умолчанию 5 минут, значение задается в миллисекундах). До обнаружения что кафка не доступна проходит время (По умолчанию
они = 60000), в течении этого времени библиотека будет копить сообщения, чтобы избегать большого накапливания данных,
рекомендуется менять еще 2 параметра: 
**audit.kafka.producer.properties.max.block.ms** и 
**audit.standin-kafka.producer.properties.max.block.ms**.

С помощью флага <strong>audit.all-block</strong> можно изменить поведение по умолчанию. При значении true все события будут блокироваться, если для них не прописан block-settings.типСобытия.

### Правила управления блокировкой<a name="правила-управления-блокировкой"></a>

| Значение audit.all-block | Включен модуль, реализующий отключение блокировки (tsau-audit-lib-blocking-context) | Блокировка аудируемого метода, если Kafka недоступна  (audit-events.block-settings.типСобытия) | Выполнение аудируемого метода |
|--------------------------|--------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|-------------------------------|
| не определено или false  | Нет  | —                         | Не блокируется  |
| не определено или false  | Да   | true                      | Блокируется     |
| не определено или false  | Да   | не определено или false   | Не блокируется  |
| true                     | Нет  | —                         | Блокируется     |
| true                     | Да   | не определено или true    | Блокируется     |
| true                     | Да   | false                     | Не блокируется  |

Поведение при не блокировке:

1. Отправлять сообщение асинхронно, если таймер не установлен или истёк:</br>
   а) Если отправлено, то вызвать callback-функцию-отправленное сообщение
2. Не отправлять сообщение и вызывать callback-функцию-неотправленное сообщение, если таймер установлен и не истёк</br>
3. Если не удалось отправить сообщение, то устанавливать таймер и вызывать:</br>
   а) callback-функцию-недоступность-Kafka</br>
   б) callback-функцию-неотправленное сообщение

Поведение при блокировке:

1. Отправлять сообщение;</br>
   a) Если отправлено, то вызвать callback-функцию-отправленное сообщение</br>
2. Если не отправлено, то аудируемый метод блокируется и вызывается:</br>
   а) callback-функцию-недоступность-Kafka</br>
   б) callback-функцию-неотправленное сообщение</br>

### Callback интерфейс для неотправленных событий<a name="callback-интерфейс-для-неотправленных-событий"></a>

В случае если есть необходимость что-либо делать с неотправленными событиями, реализован интерфейс *
*AuditSkipRecordsCallbackService:**

* **onSuccessSendInKafkaCallback** - вызывается каждый раз когда успешно отправилось сообщение в кафку;
* **onSkippingRecordCallback** - вызывается когда сообщение должно было отправлено в кафку, но не отправилось из-за
  ошибки или потому что идет таймер пропуска сообщений;
* **onExceptionSendInKafkaCallback** - вызывается когда произошла ошибка при отправке в кафку.

### Модуль хранения неотправленных событий в памяти<a name="модуль-хранения-неотправленных-событий-в-памяти"></a>

Модуль хранит неотправленные события в памяти приложения. Для подсчета количества необходимой памяти можно использовать
формулу: [ваш RPS] * [Средний размер вашего сообщений] * [Количество секунд недоступности кафки]\(~300 секунд\)

События будут храниться в памяти приложения. По расписанию будет запускаться задание (по умолчанию каждые 10 минут),
которое может начать отправку накопившихся событий. Отправка начнется, если после ошибки отправки хотя бы одно событий
успешно отправилось или если с возникновения ошибки прошло определенное количество времени (по умолчанию 5 минут).

Для подключения необходимо добавить зависимость

~~~xml
<dependency>
    <groupId>ru.vtb.omni</groupId>
    <artifactId>tsau-audit-lib-in-memory-skipped-event-context</artifactId>
</dependency>
~~~

У данного модуля есть следующие параметры:</br>

* <strong>audit.in-memory-skipped-storage.enabled</strong> - включает и выключает буфер (по умолчанию включен)
* <strong>audit.in-memory-skipped-storage.resend-cron</strong> - крон выражение по которому идет попытка переотправки
  пропущенных событий (по умолчанию каждые 10 минут)
* <strong>audit.in-memory-skipped-storage.delay-force-send</strong> - количество секунд, которое должно пройти после
  последней ошибки отправки, прежде чем попытаться отправить события (по умолчанию 300 секунд)
* <strong>audit.circular-buffer.enabled</strong> - включает и выключает циклический буфер для хранения событий (по
  умолчанию выключен)
* <strong>audit.circular-buffer.capacity</strong> - максимальное число событий, которое хранит буфер до перезаписи

Для отслеживания заполненности буфера можно воспользоваться интерфейсом BufferMetricService.

Чтобы не превысить RateLimit СС аудит, отправка событий идет в только в том случае если текущий РПС основной работы
приложения не превышает установленный РПС. Параметр для установки РПС

* <strong>audit.rps</strong> - значение с типом integer

Выбор модулей аудита для подключения<a name="выбор-модулей-аудита-для-подключения"></a>
------------------------------------

### Модули для извлечения данных события из контекста<a name="модули-для-извлечения-данных-события-из-контекста"></a>

Работа с контекстом запроса в обычном и реактивном стилях:

tsau-audit-lib-servlet-context,<br>
tsau-audit-lib-reactive-context 

Инструкция по внедрению описано в разделе [Внедрение библиотеки tsau-audit-lib](#внедрение-библиотеки-tsau-audit-lib).

### Модуль для подключения метрик<a name="модуль-для-подключения-метрик"></a>

Для подключения метрик необходимо добавить зависимость audit-lib-metric, а также предпочтительную реализацию
сборщика метрик. Например, для prometheus:
```yaml
        <dependency>
            <groupId>ru.vtb.omni</groupId>
            <artifactId>audit-lib-metric</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
```
Также настройка actuator:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus
```
Доступные метрики:

| Название                        | Описание                                                  | 
|---------------------------------|-----------------------------------------------------------|
| audit_lib_success_send_in_kafka | Количество успешно отправленных в кафку событий аудита    | 
| audit_lib_failure_send_in_kafka | Количество неуспешно отправленных в кафку событий аудита  | 


Проверка лога в вашей системе<a name="проверка-лога-в-вашей-системе"></a>
------------------------------------
При уровне логирования logging.level.ru.vtb.omni.audit

1. В логе есть

   "audit() - auditService.audit(), params = annotation.value = {auditEntityName}, EventClass = {EventClass.START}, uuid
   = {uuid}, startParams = {startParams}"

   , где

   {auditEntityName} - код события аудита,</br>
   {EventClass.START} - класс события,</br>
   {uuid} - уникальный ид события,</br>
   {startParams} - аргументы метода (тип Map).

2. Далее одно из двух (на уровне DEBUG):

   "doInAudit: audited method exception caught: "

   или

   "doInAudit: audited method done"
   "audit() - end: retVal = {значение}"

   , где {значение} равно возвращаемому вашим методом объекту.toString().

Дополнительные возможности и описания<a name="дополнительные-возможности-и-описания"></a>
=====================================

Отправка событий в кафку<a name="отправка-событий-в-кафку"></a>
------------------------

Для отправки событий в кафку с применений нашей логики Stand-In и блокировки/не блокировки событий необходимо
использовать интерфейс AuditEventSender.

Пример:

~~~java
@Service
@RequiredArgsConstructor
public class SenderService {
    private final AuditEventSender<?> kafkaAuditEventSender;

    public void sendInAuditKafka(Map<String, Object> event) {
        kafkaAuditEventSender.sendEvent(event);
    }
}
~~~

Разделение событий аудита для инстансов пода<a name="разделение-событий-аудита-для-инстансов-пода"></a>
--------------------------------------------


Резолверы NamespaceTsauResolver PodNameTsauResolver заполняют информацию о поде из которой было отправлено сообщение аудита. 
Наименование поды и namespace поды берется из переменных окружения AUDITOMNI_POD_NAME и AUDITOMNI_POD_NAMESPACE. 
Значения могут быть null, но не могут быть пустой строкой.

Можно указать из каких переменных окружения брать значения через application.yml

~~~yaml
audit:
  deployment-context:
    pod-name-environment: AUDITOMNI_POD_NAME_CUSTOM
    pod-namespace-environment: AUDITOMNI_POD_NAMESPACE_CUSTOM
~~~

Настройка TLS соединения в tsau-audit-lib<a name="настройка-tls-соединения-в-tsau-audit-lib"></a>
-------------------------------------------

Стоит отметить, что keystore-type, truststore-type по умолчанию имеют значение JKS. Если ваши кейсторы имеют расширение
.pfx или .p12, то нужно задать значение PKCS12.

Пример настройки:

~~~yaml
audit:
  kafka:
    bootstrap-servers: localhost:9092
    security:
      protocol: SSL
    ssl:
      # путь к keystore
      # для локального запуска требуется указать абсолютный путь
      key-store-location: file:kafka-certificates/client.keystore.jks
      key-store-type: 'JKS'
      key-store-password: 12345678
      # путь к truststore
      # для локального запуска требуется указать абсолютный путь
      trust-store-location: file:kafka-certificates/client.truststore.jks
      trust-store-type: 'JKS'
      trust-store-password: 12345678
      key-password: ***
~~~

Также есть возможность использовать заранее настроенный SSL bundle:

~~~yaml
audit:
  kafka:
    bootstrap-servers: localhost:9092
    security:
      protocol: SSL
    ssl:
      bundle: bundle-name
~~~

При деплое в OpenShift вам нужно примонтировать сертификаты способом принятом в вашем CI/CD и в параметрах location
указать актуальное значение. То же относится и к паролям кейсторов.

Настройка параметров событий ИБ для передачи в ОКС<a name="настройка-параметров-событий-иб-для-передачи-в-окс"></a>
-------------------------------------------

Для передачи события в ОКС необходимо обязательно заполнить поле **oks_event**. 
Возможные значения для полей ОКС можно посмотреть на странице https://sfera.inno.local/knowledge/pages?id=1863193 в таблице 2.1
Поле **oks_event** обязательно необходимо заполнять только через статичное опредление.
Например:

```yaml
audit-events:
  auditEventCodeList:
    - event-code: "TSAU_MAIN"
      schema: "auth_internal"
      audit-event-general:
        oks_event: "21"
```

Остальные поля, необходимые для передачи в ОКС, могут быть заполнены как через статическое определение, так и через динамическое (через резолверы).
Перечень полей ОКС: oks_telco, oks_payment.
Эти поле будут сериализованы JSON и поместятся в поле **oper_description**

Пример резолвера
```java
@Service
public class OksPaymentResolver implements AuditFieldResolver {
    @Override
    public Object get(String eventCode, AudLibEventClass audLibEventClass, AuditMethodParams auditMethodParams, Map<String, Object> currentFields) {
        if (auditMethodParams.getParams().get("transactionId") != null){
            String trId = auditMethodParams.getParams().get("transactionId").toString();
            switch (trId){
                case "СБП без QR Code":
                    return "1";
                case "СБП через QR Code":
                    return "2";
                case "СБП C2B":
                    return "3";
                case "СБП B2C":
                    return "4";
                case "Перевод по номеру телефона":
                    return "5";
            }
        }
        return null;
    }

    @Override
    public String getFieldName() {
        return "oks_payment";
    }
}

```

Включение health-check кафки<a name="включение-health-check-кафки"></a>
----------------------------

Есть возможность включить health check кафки в библиотеке аудита.

**Параметры управления health check кафки:**

~~~yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-components: always
  health:
# Дефолтные чеки можно отключить
    defaults.enabled: false
# Чек кафки
    audit.kafka.enabled: true
~~~

**url_audit/actuator/health**

И тогда по "url_audit/actuator/health"
Будет результат:

~~~json
{
  "status": "UP",
  "components": {
    "auditServiceKafka": {
      "status": "UP"
    }
  }
}
~~~

Параметры<a name="параметры"></a>
==================

|Параметр | Тип | Описание | Значение по умолчанию |
|---|--------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|-------------------------------|
|ms.properties.info-system-code | String | Код ИС | -     |
|ms.properties.info-system-id | String (только цифры) | Id ИС | -     |
|audit.enabled| boolean | Включение\отключение аудита. Если false то процесса формирования сообщения не будет | true  |
|audit.standin-kafka.enabled| boolean | Включение\отключение резервной кафки (см. "Настройка резервной кафки аудита (StandIn)") | false  |
|audit.kafka.topic| String | Наименование топика в который будут отправлятся сообщения аудита | audit-v2  |
|audit.deployment-context.pod-name-environment| String | Наименование перменной окружения из которой берется наименование поды (см. "Разделение событий аудита для инстансов под") | AUDITOMNI_POD_NAME     |
|audit.deployment-context.pod-namespace-environment| String | Наименование перменной окружения из которой берется наименование неймспейса (см. "Разделение событий аудита для инстансов под")   | AUDITOMNI_POD_NAMESPACE  |
|audit.default-resolver.sub| String  | Значение которое будет использовать LoginPropertiesTsauResolver | -  |
|audit.default-resolver.channel| String | Значение которое будет использовать ChannelPropertiesTsauResolver | -  |
|audit.blocking.blocking-skip-time| long | Время в миллисекундах, сколько библотека не будет пытаться отправлять сообщения в кафке, если она стала недоступна (см. "Настройка поведения библиотеки при недоступности СС "Аудит"") | 300000  |
|audit.all-block| boolean | Признак что все типы событий являются блокируемыми (см. "Настройка поведения библиотеки при недоступности СС "Аудит"") | false  |
|audit.in-memory-skipped-storage.enabled| boolean   | Включение\отключение модуля хранения пропущенных событий | false  |
|audit.in-memory-skipped-storage.resend-cron| Integer   | Крон выражение по которому идет попытка переотправки пропущенных событий | 0 */10 * * * *  |
|audit.in-memory-skipped-storage.delay-force-send| Integer   | Количество секунд, которое должно пройти после последней ошибки отправки, прежде чем попытаться отправить события | 300  |
|audit.develop-mode| boolean   | Включение\отключение режима разработчика (при включении в логах будет писаться возвращаемое значение аудируемого метода) | false  |
|audit.gateway.client.trusted-proxies-count| Integer | Количество доверенных прокси | 0 |
|audit.ip-port-custom-header-resolver| boolean | Включение\отключение резолвера XForwardedForResolverList для модифицированного заголовка **X-Forwarded-For** | false |

Параметры кафки<a name="параметры-кафки"></a>
==================

Для конфигурации кафки используется класс KafkaProperties из Spring Kafka. Для параметров Spring Kafka используется
префикс audit.kafka для основной кафки и audit.standin-kafka для резервной. Ниже описаны основные параметры

| Параметр                 | Тип          | Описание                                                   | Значение по умолчанию |
|--------------------------|--------------|------------------------------------------------------------|-----------------------|
| properties.slider.enable | boolean      | Включение\отключение слайдера, который ищет живые партиции | true                  |
| bootstrap-servers        | List<String> | Адреса брокеров кафки                                      | -                     |
| security.protocol        | String       | Протокол подключения SSL или PLAINTEXT                     | -                     |
| ssl.key-store-location   | String       | Путь к key store с сертификатами для подключения к кафке   | -                     |
| ssl.key-store-type       | String       | Тип key store                                              | -                     |
| ssl.key-store-password   | String       | Пароль key store                                           | -                     |
| ssl.trust-store-location | String       | Путь к trust store с сертификатами для подключения к кафке | -                     |
| ssl.trust-store-type     | String       | Тип trust store                                            | -                     |
| ssl.trust-store-password | String       | Пароль trust store                                         | -                     |
| ssl.key-password         | String       | Пароль к закрытому ключу                                   | -                     |

Известные проблемы<a name="известные-проблемы"></a>
==================

1. Если пытаться подключиться к TLS-порту Kafka по протоколу PLAINTEXT (т.е. не настроено подключение по TLS), то из-за
   ошибки в org.apache.kafka клиенте у вас будет ошибка OutOfMemory в приложении. Чтобы решить, необходимо настроить
   TLS.
