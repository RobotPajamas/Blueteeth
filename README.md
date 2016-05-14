[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Blueteeth-blue.svg?style=flat)](http://android-arsenal.com/details/1/3512)

# Blueteeth

## What Is Blueteeth?

Blueteeth is a simple, lightweight library intended to take away some of the cruft and tediousness of using the Android BLE API. I wrote about it originally here: [http://www.sureshjoshi.com/mobile/bluetooth-bluetooths-blueteeth/](http://www.sureshjoshi.com/mobile/bluetooth-bluetooths-blueteeth/), with a more recent update at [http://www.sureshjoshi.com/mobile/blueteeth-for-android/](http://www.sureshjoshi.com/mobile/blueteeth-for-android/).

It was inspired by the simplicity and ease-of-use of [LGBluetooth](https://github.com/l0gg3r/LGBluetooth) on iOS.

## High-Level

An underlying motivator of this BLE library was to build something that didn't require platform-specific hacks. 

Blueteeth fixes several Android-platform-specific problems, in terms of calls sometimes working and sometimes not. Under the hood, all calls are initiated from the main thread... This pattern helps solve some manufacturer-specific defects in Android BLE implementations ([looking at you, Samsung](https://stackoverflow.com/questions/20069507/gatt-callback-fails-to-register)).

In the `.connect()` call, I have exposed the autoReconnect parameter which allows an underlying Bluetooth connection to do exactly what it suggests. However, I recommend not using it, and I might remove it entirely. [This StackOverflow post](https://stackoverflow.com/questions/22214254/android-ble-connect-slowly/23749770#23749770) explains it well, but essentially, autoReconnect is a very slow, low-power reconnect... I've noticed it can take a minute or two to actually re-connect, so it doesn't function like most developers would expect.

Additionally, when you're done with the BlueteethDevice object, call `.close()` on it, in order to free up resources (there is a finalizer on the BlueteethDevice object, but don't rely on it running). Calling `.close()` on the BLE objects is both suggested and required.

## Usage

Scan for BLE devices using the BlueteethManager singleton:

```java
BlueteethManager.with(this).scanForPeripherals(DEVICE_SCAN_MILLISECONDS, blueteethDevices -> {
    // Scan completed, iterate through received devices and log their name/mac address
    for (BlueteethDevice device : blueteethDevices) {
        if (!TextUtils.isEmpty(device.getBluetoothDevice().getName())) {
            Timber.d("%s - %s", device.getName(), device.getMacAddress());
        }
    }
});
```

Initiate a connection using a BlueteethDevice:

```java 
mBlueteethDevice.connect(shouldAutoreconnect, isConnected -> {
    Timber.d("Is the peripheral connected? %s", Boolean.toString(isConnected));
});
```

Discover Bluetooth services and characteristics:

```java 
mBlueteethDevice.discoverServices(response -> {
    if (response != BlueteethResponse.NO_ERROR) {
        Timber.e("Discovery error - %s",  response.name());
        return;
    }
    Timber.d("Discovered services... Can now try to read/write...");
});
``` 

Write to a connected BlueteethDevice:

```java
mBlueteethDevice.writeCharacteristic(new byte[]{1, 2, 3, 4}, characteristicUUID, serviceUUID, response -> {
    if (response != BlueteethResponse.NO_ERROR) {
        Timber.e("Write error - %s",  response.name());
        return;
    }
    Timber.d("Characterisic Written...");
})
```

Read from a connected BlueteethDevice:

```java 
mBlueteethDevice.readCharacteristic(characteristicUUID, serviceUUID, (response, data) -> {
    if (response != BlueteethResponse.NO_ERROR) {
        Timber.e("Read error - %s",  response.name());
        return;
    }
    Timber.d("Just read the following data... %s",  Arrays.toString(data));
});
```

Convenience methods to connect (if not connected), and read/write... This will NOT automatically disconnect, however, so you will remain disconnected unless you try to disconnect in the callback:
 
```java
BlueteethUtils.writeData(new byte[]{1, 2, 3, 4}, characteristicUUID, serviceUUID, mBlueteethDevice, response -> {
    if (response != BlueteethResponse.NO_ERROR) {
        Timber.e("Write error - %s",  response.name());
        return;
    }
    Timber.d("Connected to and wrote characteristic...");
});
```

```java
BlueteethUtils.read(characteristicUUID, serviceUUID, mBlueteethDevice, (response, data) -> {
    if (response != BlueteethResponse.NO_ERROR) {
        Timber.e("Read error - %s",  response.name());
        return;
    }
    Timber.d("Just connected and read the following data... %s",  Arrays.toString(data));
});
```

Check out the sample app in `blueteeth-sample/` to see the API in action. 


## Future Directions

### Auto-Reconnect

As mentioned above, the stack-level auto-reconnect implementation is pretty useless because of how long the reconnect process takes. I might leave the existing API, however, use a different underlying implementation to allow fast(er) reconnects.

### Queues

As mentioned in [my original Blueteeth blog post](http://www.sureshjoshi.com/mobile/bluetooth-bluetooths-blueteeth/), I hate Callback Hell (or rightward drift), but I haven't yet solved that in this library. The current usage for chaining calls is still, unfortunately, callbacks in callbacks. 

### Mocks and Tests

Out-of-band of this repo, I have been working on a small mocking framework to a) allow unit/functional testing of classes that call BlueteethManager (singleton), and b) allow a runtime implementation of Blueteeth, so it can be tested in a simulator, and preloaded with 'fake' results. 

a) is quick and easy, b) is hard and painful.

I will incorporate that code into this repo when it is stable.

### Reactive Everything!

Now that this library is released and progressively becoming more stable, the next step in the process is to create Reactive bindings (RxAndroid bindings specifically). They will be created in a separate repo, so that there isn't a forced, heavy dependency on the Rx framework in any app that just wants to use Blueteeth.

## Download

```groovy
compile 'com.robotpajamas.blueteeth:blueteeth:0.2.0'
```

## Issues

Lastly, I have a few lingering issues that I want to fix in the near future. 1 bug, and a few points just for my sanity: [https://github.com/RobotPajamas/Blueteeth/issues](https://github.com/RobotPajamas/Blueteeth/issues)

## License

The Apache License (Apache)

    Copyright (c) 2016 Robot Pajamas

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
