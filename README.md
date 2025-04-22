# SWT for jpms

I have try to use Eclipse to build an application with my framework,
but Eclipse swt export a same java package with different module name
that is not allowed in JPMS system.

with old version of java like 1.8 (java 8), it is easy to make it working on different os 
by replace swt jars in classpath and that does not work with JPMS anymore, it is the 
problem what this project want to resolve.

我很早之前就尝试过整合SWT到JPMS模块中，使之正式成为我的应用程序模型之一，
但是JPMS不允许两个模块导出相同的package，这直接导致了SWT完全无法融入JPMS体系。

SWT对于任意平台，都有同样的package，这样的设计使得不同的平台直接在classpath中
更换适当的库就能完美的运行，但是JPMS下，不同平台的jar具备不同的模块名，而它们都会
导出同一个package，这就是本项目想要解决的问题。

## swt-plugin

first of all, please install this plugin in your local maven repository,
it will repackage swt jars and provide a same module name for different platform,
that we can use maven profiles select the jar with current os, every platform jar has 
a same module name make it easily to working with different platform with JPMS.

使用本项目需要首先在本地安装此Plugin，直接使用Maven install安装它即可，这个
Plugin的作用是重新打包SWT的平台库，它的目的是使每一个平台的SWT都具备同一个模块名，
这样，就像老版本通过切换classpath的SWT库就能轻松适应不同平台那样使之正常运行。

## swt-platform

each project is a swt platform library reference to swt official maven dependency,
using swt-plugin that we can repackage them and install into local maven repository.

每一个project都是一个平台的SWT库，它们引用SWT官方的依赖库并且通过本项目的swt-Plugin重新
打包，使每一个平台的库都具备同样的模块名称。

