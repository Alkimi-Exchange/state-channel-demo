# Tessellation Demo.

Simple integrations with the tessellation directed acyclic graph (dag).


## Tessellation
This project depends on the constellation tessellation libraries. See:

  https://github.com/constellation-Labs/tessellation

The project has been developed against release `v0.11.2`:

  https://github.com/Constellation-Labs/tessellation/releases/tag/v0.11.2

As of the time of writing, in order to build this project you will need to clone, assemble and locally release this version of the tessellation code. 
Assuming you have a directory `/workspace` then the required steps will be something like the following:

```
git clone https://github.com/Constellation-Labs/tessellation.git
cd tessellation
git checkout v0.11.2
sbt assembly
sbt publishM2 
```

You will need at least one tessellation L0 (genesis) node running in order to test this service - please refer to the tessellation documentation.


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

| *Resource*                            | *Method* | *Query Paramaters* | *Description*                                                                                                        |
|---------------------------------------|----------|--------------------|----------------------------------------------------------------------------------------------------------------------|
| ```/demo/ping```                      | GET      |                    | Tests server availability.                                                                                           |
| ```/demo/global-snapshots/:ordinal``` | GET      |                    | Returns a string representation of the global-snapshot identified by the ordinal.                                    |
| ```/demo/transactions/:ordinal```     | GET      |                    | Returns any demo transactions persisted in the global snapshot with the supplied ordinal.                            |
| ```/demo/state-channel-snapshot```    | POST     | lastSnapshotHash   | Returns a signed copy of the supplied transaction or sequence of transactions if valid; else BadRequest              |

Note that `/demo/state-channel-snapshot` demonstrates simple validation of payloads.

The `POST` payload is a `json` body encoding either a single `DemoTransaction` or a sequence of `DemoTransactions`, e.g.

    {
        "txnid": "txnid1",
        "resourceid": "resource_id",
        "data1": 1000
    }

The validation rules applied are that:
* the 'txnid" field length must be > 5 characters; and
* the 'data1" field value must be > 0


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


To create a signed state channel binary snapshot payload from a single sample transaction using the default last snapshot hash:
```
curl -v -X POST http://localhost:19000/demo/data-transactions/sign -H 'Content-Type:application/json' -H "Accept:application/json" -d @examples/DemoTransaction.json
```
The payload can be submitted to `tessellation` L0


To create a signed state channel binary snapshot payload from a single sample transaction using a specified last snapshot hash:
```
curl -v -X POST http://localhost:19000/demo/data-transactions/sign?lastSnapshotHash=649738443ff34068f267428420e42cbcc79824bea7e004faffbca67fddad3f08 -H 'Content-Type:application/json' -H "Accept:application/json" -d @examples/DemoTransaction.json
```
The payload can be submitted to `tessellation` L0


To create a signed state channel binary snapshot payload from multiple sample transactions using the default last snapshot hash:
```
curl -v -X POST http://localhost:19000/demo/data-transactions/sign?lastSnapshotHash=649738443ff34068f267428420e42cbcc79824bea7e004faffbca67fddad3f08 -H 'Content-Type:application/json' -H "Accept:application/json" -d @examples/MultipleDemoTransactions.json
```
The payload can be submitted to `tessellation` L0


To submit invalid sample transactions to `/data-transactions/sign` in order to see a validation failure with a subsequent `BadRequest` response:
```
curl -v -X POST http://localhost:19000/demo/data-transactions/sign?lastSnapshotHash=649738443ff34068f267428420e42cbcc79824bea7e004faffbca67fddad3f08 -H 'Content-Type:application/json' -H "Accept:application/json" -d @examples/InvalidDemoTransactions.json
```


To retrieve any data-transactions you might have persisted in the tessellation global snapshot ordinal 0:
```
curl -i http://localhost:19000/demo/data-transactions/0
```


To see the starting token balance of wallet 1:
```
curl -i http://localhost:19000/demo/token-balances/DAG45MPJCa2RsStWfdv8RZshrMpsFHhnsiHN7kvL
```


To see the starting token balance of wallet 2:
```
curl -i http://localhost:19000/demo/token-balances/DAG45MPJCa2RsStWfdv8RZshrMpsFHhnsiHN7kvP
```


To see the starting token balance of wallet 3:
```
curl -i http://localhost:19000/demo/token-balances/DAG45MPJCa2RsStWfdv8RZshrMpsFHhnsiHN7kvT
```


To make a transaction transferring 100 token from wallet 1 to wallet 2:
```
curl -v -X POST http://localhost:19000/demo/token-transactions  -H 'Content-Type:application/json' -H "Accept:application/json" -d @examples/TokenTransaction.json
```


Use the returned transaction id to view the persisted transaction:
```
curl -i http://localhost:19000/demo/token-transactions/b74a1fe3-1b89-432a-9aa7-9004e490cd31
```

You can see all the token transactions for a wallet address like this:
``` 
curl -i http://localhost:19000/demo/token-transactions/wallet/DAG45MPJCa2RsStWfdv8RZshrMpsFHhnsiHN7kvL
```

You can also view the balance for wallets 1 and 2 again:
``` 
curl -i http://localhost:19000/demo/token-balances/DAG45MPJCa2RsStWfdv8RZshrMpsFHhnsiHN7kvL
curl -i http://localhost:19000/demo/token-balances/DAG45MPJCa2RsStWfdv8RZshrMpsFHhnsiHN7kvP
```
