/** Resolves the library through its Automatic-Module-Name on the module path. */
module teamspeak3.consumer {
    requires com.github.theholywaffle.teamspeak3;
    requires org.slf4j;
    requires java.logging;
    // TS3WebQuery uses the JDK HTTP client, which an automatic module does not resolve on its own.
    requires java.net.http;

    uses org.slf4j.spi.SLF4JServiceProvider;
}
