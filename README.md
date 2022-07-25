# Tessellation Demo.

Simple integrations with the tessellation directed acyclic graph (dag).

## Build and run
To compile:
```
sbt compile
```

To test:
```
sbt test
```

To assemble an executable jar:
```
sbt assembly
```


## API

| *Resource*                            | *Method* | *Query Paramaters* | *Description*                                                                             |
|---------------------------------------|----------|--------------------|-------------------------------------------------------------------------------------------|
| ```/demo/ping```                      | GET      |                    | Tests server availability.                                                                |
| ```/demo/global-snapshots/:ordinal``` | GET      |                    | Returns a string representation of the global-snapshot identified by the ordinal.         |
| ```/demo/transactions/:ordinal```     | GET      |                    | Returns any demo transactions persisted in the global snapshot with the supplied ordinal. |
| ```/demo/state-channel-snapshot```    | POST     | lastSnapshotHash   | Returns a signed copy of the supplied transaction or sequence of transactions.            |


## Configuration
The service uses `decline` for configuration.

    https://github.com/bkirwi/decline 

The following environment variables must be set:

    CL_KEYSTORE
    CL_KEYALIAS
    CL_PASSWORD

This can be done on a `nix` system using an `export` command e.g.: 

    export CL_KEYSTORE=$./key.p12 CL_KEYALIAS=walletalias CL_PASSWORD=welcome123

The following additional configuration items are currently supported.  

| *Item*                  | *Default value*                          |
|-------------------------|------------------------------------------|
| tessellation-url-prefix | http://localhost:9000                    |
| state-channel-address   | DAG45MPJCa2RsStWfdv8RZshrMpsFHhnsiHN7kvX |
| ip                      | 127.0.0.1                                |
| public-port             | 19000                                    |
| p2p-port                | 19001                                    |
| cli-port                | 19002                                    |

These items have default values which can be overridden by supplying a command line argument with the item name.

For example to override the public port if the assembled jar file is named `acc.jar`:

    java -cp acc.jar com.teseellation.demo.Main run-demo --public-port 8000


## Key store
The service requires a keystore (`key.p12` file) and also must be configured to find the keystore.

A sample keystore used by the tests is located in `src/test/resources`. This can be used for some use cases when running locally.

To generate a keystore use the `constellation keystore tool`. For example to create a keystore in the `/keystore` directory:

    cd [PATH_TO_TESSELLATION_REPO]
    sbt assembly
    cp /modules/keytool/target/scala-2.13/tessellation-keytool-assembly-XXX.jar /keystore/keytool.jar
    cd /keystore
    export CL_KEYSTORE=/keystore/key.p12 CL_KEYALIAS=walletalias CL_PASSWORD=welcome123
    java -jar keytool.jar generate


## Sample commands
To run from the project base directory you must specify the path to a working keystore (see above). e.g.

```
sbt "run run-demo" 
```

To run an assembled jar which has been named `acc.jar`, using the default port 19000:

```
java -cp acc.jar com.teseellation.demo.Main run-demo
```

To run an assembled jar which has been named `acc.jar` and override the tessellation url:

```
java -cp acc.jar com.teseellation.demo.Main run-demo  --tessellation-url-prefix http://localhost:8000
```


To test service availability:
```
curl -i http://localhost:19000/demo/ping
```


To see the global snapshot identified by the ordinal 0:
```
curl -i http://localhost:19000/demo/global-snapshots/0
```


To create a state channel snapshot from a single sample transaction using the default last snapshot hash:
```
curl -v -X POST http://localhost:19000/demo/state-channel-snapshot -H 'Content-Type:application/json' -H "Accept:application/json" -d @examples/DemoTransaction.json
```


To create a state channel snapshot from a single sample transaction using a specified last snapshot hash:
```
curl -v -X POST http://localhost:19000/demo/state-channel-snapshot?lastSnapshotHash=649738443ff34068f267428420e42cbcc79824bea7e004faffbca67fddad3f08 -H 'Content-Type:application/json' -H "Accept:application/json" -d @examples/DemoTransaction.json
```


To create a state channel snapshot from multiple sample transactions using the default last snapshot hash:
```
curl -v -X POST http://localhost:19000/demo/state-channel-snapshot -H 'Content-Type:application/json' -H "Accept:application/json" -d @examples/MultipleDemoTransactions.json
```


To retrieve all transactions persisted in ordinal 1:
```
curl -i http://localhost:19000/demo/transactions/1
```