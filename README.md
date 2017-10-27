# JBinary [![](https://jitpack.io/v/AlbinEriksson/JBinary.svg)](https://jitpack.io/#AlbinEriksson/JBinary)
JBinary is a binary file framework for Java 1.8.
It makes I/O of binary data much simpler. No need for complex bitwise operations!
## What does it do?
It reads and writes both files and byte arrays. The abstract stream classes are set up to easily make customized streams.
## How to setup
You have to first setup either Maven or Gradle for your project.
### For Maven:
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
		<version>2.0.0</version>
	</dependency>
</dependencies>
```
### For Gradle:
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
	compile 'com.github.AlbinEriksson:JBinary:2.0.0'
}
```
### Development versions
The version number you see above is the latest release version, but you can also replace it with the short commit hash, if you want development versions.
## Contributing
Feel free to contribute with pull requests, issues, ideas etc. You can contact me through e-mail. Any help is very appreciated, as this is a solo project.
## Still need help?
If anything was unclear or missing in this readme, please contact me so I can fix that.
## Example
A date and time can be stored like this: `yyyyyyyy yyyMMMMd ddddHHHH Hmmmmmms sssss---`
Each letter represents a bit which is needed for each value. "y" is for year, "M" is for month, etc. The "-" is unused. The space represents how the bytes are separated. This might be tedious to read using standard bitwise operators. But with JBinary, the code looks something like this:
```java
// The byte array data is just an example. It is just as easy to write it using BitArrayOutputStream.
try(BitArrayInputStream in = new BitArrayInputStream(new byte[]
	{(byte)0xFC, (byte)0x11, (byte)0xC5, (byte)0x56, (byte)0xF0}))
{
	// Here's the code of interest. It's only one simple line per value!
	int year   = in.readAsInt(11); // 0 to 2047
	int month  = in.readAsInt(4);  // 0 to 15
	int day    = in.readAsInt(5);  // 0 to 31
	int hour   = in.readAsInt(5);  // 0 to 31
	int minute = in.readAsInt(6);  // 0 to 63
	int second = in.readAsInt(6);  // 0 to 63
	
	System.out.printf("%d-%02d-%02d %02d:%02d:%02d",
		year, month, day, hour, minute second);
}
catch(IOException e)
{
	e.printStackTrace();
}
```
That code prints "2016-08-28 10:43:30" which is the time as of writing this.