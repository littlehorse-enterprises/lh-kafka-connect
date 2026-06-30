# LittleHorse Connectors for Kafka Connect

## WfRunSinkConnector Configurations

``lhc.api.host``
  The bootstrap host for the LittleHorse Server.

  * Type: string
  * Importance: high

``lhc.api.port``
  The bootstrap port for the LittleHorse Server.

  * Type: int
  * Importance: high

``wf.spec.name``
  The name of the WfSpec to run.

  * Type: string
  * Importance: high

``lhc.api.protocol``
  The bootstrap protocol for the LittleHorse Server.

  * Type: string
  * Default: PLAINTEXT
  * Valid Values: [PLAINTEXT, TLS]
  * Importance: high

``lhc.tenant.id``
  Tenant ID which represents a logically isolated environment within LittleHorse.

  * Type: string
  * Default: default
  * Importance: medium

``transient.errors.tolerance``
  How to handle records that fail with a transient (retriable) gRPC error such as network issues or the server being temporarily unavailable. When 'transients' the record is retried by Kafka Connect and never sent to the DLQ; when 'none' the transient error is treated like any other error and handled according to the errors.tolerance setting.

  * Type: string
  * Default: transients
  * Valid Values: [none, transients]
  * Importance: medium

``lhc.ca.cert``
  Optional location of CA Cert file that issued the server side certificates. For TLS and mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.client.cert``
  Optional location of Client Cert file for mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.client.key``
  Optional location of Client Private Key file for mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.grpc.keepalive.time.ms``
  Time in milliseconds to configure keepalive pings on the grpc client.

  * Type: long
  * Default: 45000 (45 seconds)
  * Importance: low

``lhc.grpc.keepalive.timeout.ms``
  Time in milliseconds to configure the timeout for the keepalive pings on the grpc client.

  * Type: long
  * Default: 5000 (5 seconds)
  * Importance: low

``lhc.oauth.access.token.url``
  Optional Access Token URL provided by the OAuth Authorization Server. Used by the Worker to obtain a token using client credentials flow.

  * Type: string
  * Default: null
  * Importance: low

``lhc.oauth.client.id``
  Optional OAuth2 Client Id. Used by the Worker to identify itself at an Authorization Server. Client Credentials Flow.

  * Type: string
  * Default: null
  * Importance: low

``lhc.oauth.client.secret``
  Optional OAuth2 Client Secret. Used by the Worker to identify itself at an Authorization Server. Client Credentials Flow.

  * Type: password
  * Default: null
  * Importance: low

``wf.run.parent.id``
  Optionally specify the default parent WfRunId.

  * Type: string
  * Default: null
  * Importance: low

``wf.spec.major.version``
  Optionally specify the major version of the WfSpec to run. This guarantees that the "signature" of the WfSpec (i.e. the required input variables, and searchable variables) will not change for this app.

  * Type: int
  * Default: null
  * Importance: low

``wf.spec.revision``
  Optionally specify the specific revision of the WfSpec to run. It is not recommended to use this in practice, as the WfSpec logic should be de-coupled from the applications that run WfRun's.

  * Type: int
  * Default: null
  * Importance: low

## ExternalEventSinkConnector Configurations

``external.event.name``
  The name of the ExternalEventDef.

  * Type: string
  * Importance: high

``lhc.api.host``
  The bootstrap host for the LittleHorse Server.

  * Type: string
  * Importance: high

``lhc.api.port``
  The bootstrap port for the LittleHorse Server.

  * Type: int
  * Importance: high

``lhc.api.protocol``
  The bootstrap protocol for the LittleHorse Server.

  * Type: string
  * Default: PLAINTEXT
  * Valid Values: [PLAINTEXT, TLS]
  * Importance: high

``lhc.tenant.id``
  Tenant ID which represents a logically isolated environment within LittleHorse.

  * Type: string
  * Default: default
  * Importance: medium

``transient.errors.tolerance``
  How to handle records that fail with a transient (retriable) gRPC error such as network issues or the server being temporarily unavailable. When 'transients' the record is retried by Kafka Connect and never sent to the DLQ; when 'none' the transient error is treated like any other error and handled according to the errors.tolerance setting.

  * Type: string
  * Default: transients
  * Valid Values: [none, transients]
  * Importance: medium

``lhc.ca.cert``
  Optional location of CA Cert file that issued the server side certificates. For TLS and mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.client.cert``
  Optional location of Client Cert file for mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.client.key``
  Optional location of Client Private Key file for mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.grpc.keepalive.time.ms``
  Time in milliseconds to configure keepalive pings on the grpc client.

  * Type: long
  * Default: 45000 (45 seconds)
  * Importance: low

``lhc.grpc.keepalive.timeout.ms``
  Time in milliseconds to configure the timeout for the keepalive pings on the grpc client.

  * Type: long
  * Default: 5000 (5 seconds)
  * Importance: low

``lhc.oauth.access.token.url``
  Optional Access Token URL provided by the OAuth Authorization Server. Used by the Worker to obtain a token using client credentials flow.

  * Type: string
  * Default: null
  * Importance: low

``lhc.oauth.client.id``
  Optional OAuth2 Client Id. Used by the Worker to identify itself at an Authorization Server. Client Credentials Flow.

  * Type: string
  * Default: null
  * Importance: low

``lhc.oauth.client.secret``
  Optional OAuth2 Client Secret. Used by the Worker to identify itself at an Authorization Server. Client Credentials Flow.

  * Type: password
  * Default: null
  * Importance: low

## CorrelatedEventSinkConnector Configurations

``external.event.name``
  The name of the ExternalEventDef.

  * Type: string
  * Importance: high

``lhc.api.host``
  The bootstrap host for the LittleHorse Server.

  * Type: string
  * Importance: high

``lhc.api.port``
  The bootstrap port for the LittleHorse Server.

  * Type: int
  * Importance: high

``lhc.api.protocol``
  The bootstrap protocol for the LittleHorse Server.

  * Type: string
  * Default: PLAINTEXT
  * Valid Values: [PLAINTEXT, TLS]
  * Importance: high

``lhc.tenant.id``
  Tenant ID which represents a logically isolated environment within LittleHorse.

  * Type: string
  * Default: default
  * Importance: medium

``transient.errors.tolerance``
  How to handle records that fail with a transient (retriable) gRPC error such as network issues or the server being temporarily unavailable. When 'transients' the record is retried by Kafka Connect and never sent to the DLQ; when 'none' the transient error is treated like any other error and handled according to the errors.tolerance setting.

  * Type: string
  * Default: transients
  * Valid Values: [none, transients]
  * Importance: medium

``lhc.ca.cert``
  Optional location of CA Cert file that issued the server side certificates. For TLS and mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.client.cert``
  Optional location of Client Cert file for mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.client.key``
  Optional location of Client Private Key file for mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.grpc.keepalive.time.ms``
  Time in milliseconds to configure keepalive pings on the grpc client.

  * Type: long
  * Default: 45000 (45 seconds)
  * Importance: low

``lhc.grpc.keepalive.timeout.ms``
  Time in milliseconds to configure the timeout for the keepalive pings on the grpc client.

  * Type: long
  * Default: 5000 (5 seconds)
  * Importance: low

``lhc.oauth.access.token.url``
  Optional Access Token URL provided by the OAuth Authorization Server. Used by the Worker to obtain a token using client credentials flow.

  * Type: string
  * Default: null
  * Importance: low

``lhc.oauth.client.id``
  Optional OAuth2 Client Id. Used by the Worker to identify itself at an Authorization Server. Client Credentials Flow.

  * Type: string
  * Default: null
  * Importance: low

``lhc.oauth.client.secret``
  Optional OAuth2 Client Secret. Used by the Worker to identify itself at an Authorization Server. Client Credentials Flow.

  * Type: password
  * Default: null
  * Importance: low

## FilterByFieldPredicate Configurations

``field``
  Field name.

  * Type: string
  * Importance: high

``pattern``
  Java regex pattern.

  * Type: string
  * Importance: high

``operation``
  Operation type, include or exclude message.

  * Type: string
  * Default: exclude
  * Valid Values: [include, exclude]
  * Importance: high

## JsonPathFilterPredicate Configurations

``expression``
  A JSONPath expression (starting with '$') evaluated against the record envelope {key, value, headers}. The record matches when the result is truthy: a true boolean, a non-empty match list or object, or any other non-null value (e.g. an existence check); it does not match when the result is null, false, or an empty match. Combine with the transform's 'negate' option to invert the result.

  * Type: string
  * Importance: high

## JsonPathMapperTransform Configurations

``mapping``
  Defines a mapping written into the operating domain. Each mapping is its own property: the bare ``mapping`` targets the whole domain, while ``mapping.<path>`` (a dot-separated path such as ``mapping.pilot.vehicle.model``) builds nested objects; for the ``$Headers`` variant the whole path is a single, flat header name. The value must be a JSONPath expression (starting with '$') evaluated against the record envelope ``{key, value, headers}``, and functions such as ``concat`` and ``sum`` are supported. Use the ``$Key``, ``$Value`` or ``$Headers`` nested variant to choose whether the record key, value or headers are rebuilt. The operating domain is built from scratch, so unmapped fields are dropped.

  * Type: string
  * Default: null
  * Importance: high

## LiteralMapperTransform Configurations

``mapping``
  Defines a mapping written into the operating domain. Each mapping is its own property: the bare ``mapping`` targets the whole domain, while ``mapping.<path>`` (a dot-separated path) builds nested objects; for the ``$Headers`` variant the whole path is a single, flat header name. The value is a constant whose type is inferred (an integer, a double, ``true``/``false`` as a boolean, ``null`` as a null value, otherwise a string; wrap the value in double quotes to force a string). Use the ``$Key``, ``$Value`` or ``$Headers`` nested variant to choose whether the record key, value or headers are written. The constants are merged onto the existing domain, overriding any fields with the same name.

  * Type: string
  * Default: null
  * Importance: high

``implicit.casting.enabled``
  When ``true`` (the default), each mapping value is parsed into an inferred type (an integer, a double, ``true``/``false`` as a boolean, ``null`` as a null value, otherwise a string). When ``false``, every value is kept as its original string with no type inference.

  * Type: boolean
  * Default: true
  * Importance: medium
