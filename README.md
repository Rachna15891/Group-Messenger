# Group-Messenger
A Group Messenger app for Android which guarantees Total and FIFO Ordering With Failure Detection

This project implemenats ISIS algorithm for maintaining TOTAL order and uses Priority queues based on the sequences for any particular emulator/device to guarantee FIFO order.In addition, it implements a key-value table that each device uses to individually store all messages on its local storage.

This project is divided into two parts:
1). Without failure of any emulator/device
2). With failure of any random node 

The provider has two columns.

  - The first column is “key”. This column is used to store all keys.
  - The second column is “value”. This column is used to store all values.
  - String datatype is used for storing all keys and values.

Only two methods insert() and query() are implemented currently. Since the column names are “key” and “value”, any app should be able to insert a pair as in the following example:

```sh
ContentValues keyValueToInsert = new ContentValues();
// inserting <”key-to-insert”, “value-to-insert”>
keyValueToInsert.put(“key”, “key-to-insert”);
keyValueToInsert.put(“value”, “value-to-insert”);
Uri newUri = getContentResolver().insert(
    providerUri,    // assume we already created a Uri object with our provider URI
    keyValueToInsert
);
```

If there’s a new value inserted using an existing key, we keep only the most recent value. History of values under the same key is not preserved, its overwritten.

Similarly, any app can read a pair from the provider with query().

Provider can answer queries as in the following example:

```sh
Cursor resultCursor = getContentResolver().query(
    providerUri,    // assume we already created a Uri object with our provider URI
    null,                // no need to support the projection parameter
    “key-to-read”,    // we provide the key directly as the selection parameter
    null,                // no need to support the selectionArgs parameter
    null                 // no need to support the sortOrder parameter
);
```

The app multicasts every user-entered message to all app instances (including the one that is sending the message). The app uses B-multicast. The algorithm in the application provides a total-causal ordering.

The app opens one server socket that listens on 10000. Use the python scripts to create, run and set the ports of the AVD’s, by using the following commands : 

```sh
python create_avd.py 5
python run_avd.py 5
python set_redir.py 10000
```

The redirection ports for the AVD’s will be-

```sh
emulator-5554: “5554” - 11108
emulator-5556: “5556” - 11112
emulator-5558: “5558” - 11116
emulator-5560: “5560” - 11120
emulator-5562: “5562” - 11124
```

Every message is stored in the provider individually by all app instances. Each message is stored as a pair.

The key is the final delivery sequence number for the message; the value is the actual message.

The delivery sequence number starts from 0 and increase by 1 for each message.
