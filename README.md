#JBinary [![](https://jitpack.io/v/AlbinEriksson/JBinary.svg)](https://jitpack.io/#AlbinEriksson/JBinary)
JBinary is a binary file framework for Java 1.8.
It aims to be a simple way to work with binary files, as a helper in your code.
##What does it do?
Currently, it is used for reading sequences of bytes and read them bit by bit. An example exists below.
##How to setup
You have to first setup either Maven or Gradle for your project.
###For Maven:
Add the jitpack.io repository to your pom.xml file:
```xml
<repositories>
	<repository>
		<id>jitpack.io</id>
		<url>https://jitpack.io</url>
	</repository>
</repositories>
```
Then, add the JBinary dependency:
```xml
<dependencies>
	<dependency>
		<groupId>com.github.AlbinEriksson</groupId>
		<artifactId>JBinary</artifactId>
		<version>1.1</version>
	</dependency>
</dependencies>
```
###For Gradle:
Add the jitpack.io repository to your root build.gradle file:
```gradle
allprojects {
	repositories {
		maven { url "https://jitpack.io" }
	}
}
```
Then, add the JBinary dependency:
```gradle
dependencies {
	compile 'com.github.AlbinEriksson:JBinary:1.1'
}
```
###Development versions
The version number you see above is the latest release version, but you can also replace it with the short commit hash, if you want development versions.
##Contributing
Feel free to contribute with pull requests, issues, ideas etc. You can contact me through e-mail. Any help is very appreciated, as this is a solo project.
##Still need help?
If anything was unclear or missing in this readme, please contact me so I can fix that.
##Example
A date and time can be stored like this: `yyyyyyyy yyyMMMMd ddddHHHH Hmmmmmms sssss---`
Each letter represents a bit which is needed for each value. "y" is for year, "M" is for month, etc. The "-" is unused. The space represents how the bytes are separated. As you can see, this might be a little difficult to read using standard bitwise operators. But with JBinary, the code looks something like this:
```java
BitReader bit = new BitReader(new byte[]
{(byte)0xFC, (byte)0x11, (byte)0xC5, (byte)0x56, (byte)0xF0});
int year   = bit.getNextInt(11); // 11 bits in a year (0 to 2047),
int month  = bit.getNextInt(4);  //  4 bits in a month (0 to 15),
int day    = bit.getNextInt(5);  //  5 bits in a day (0 to 31),
int hour   = bit.getNextInt(5);  //  5 bits in an hour (0 to 31),
int minute = bit.getNextInt(6);  //  6 bits in a minute (0 to 63) and
int second = bit.getNextInt(6);  //  6 bits in a second (0 to 63).
bit.addToBitIndex(3);            //  Unused.
System.out.printf("%d-%02d-%02d %02d:%02d:%02d",
	year, month, day, hour, minute, second);
```
That code prints "2016-08-28 10:43:30" which is the time as of writing this.