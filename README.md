
# What is CBLiteEktorp?

CBLiteEktorp is something to make it easy to access a Couchbase Lite mobile database via Ektorp.  

# How to use CBLiteEktorp in your Android Studio project

This assumes that `~/MyProject` is an Android Studio project which contains a settings.gradle file

*Step #1*: checkout CBLiteEktorp as a git submodule:

```
$ cd ~/MyProject
$ git submodule add https://github.com/couchbaselabs/CBLiteEktorp.git CBLiteEktorp
```

*Step #2*: add an entry to settings.gradle.

Before change:

```
include ':CouchChatAndroid', ':CBLite'
``` 

After change:

```
include ':CouchChatAndroid', ':CBLite', ':CBLiteEktorp'
```