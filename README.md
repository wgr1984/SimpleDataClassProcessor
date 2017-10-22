[![Build Status](https://travis-ci.org/wgr1984/SimpleDataClassProcessor.svg?branch=master)](https://travis-ci.org/wgr1984/SimpleDataClassProcessor)
[ ![Download](https://api.bintray.com/packages/wgr1984/SimpleDataClasses/SimpleDataClassProcessor/images/download.svg) ](https://bintray.com/wgr1984/SimpleDataClasses/SimpleDataClassProcessor/_latestVersion)

# SimpleDataClassProcessor
AnnotationProcessor to create Kotlin like data classes with the help of Google's AutoValue

# How to use
As project currenlty not published to major maven repos please add:
```
repositories {
   maven { url "https://dl.bintray.com/wgr1984/SimpleDataClasses"}
}
```
and the following two dependecies:
```
annotationProcessor "de.wr.simpledataclasses:simpleDataClassesProcessor:0.1"
provided "de.wr.simpledataclasses:libSimpleDataClasses:0.1"
```

Todo:
- [ ] Publish to jcenter
- [ ] Provide samples
- [ ] support auto-value-gson default values
