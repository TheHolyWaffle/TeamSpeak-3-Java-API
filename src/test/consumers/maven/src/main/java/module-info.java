/** Resolves the library through its Automatic-Module-Name on the module path. */
module teamspeak3.consumer {
    requires com.github.theholywaffle.teamspeak3;
    requires org.slf4j;
    requires java.logging;
    // TS3WebQuery's JDK HTTP client and Gson are named modules that an automatic module does not resolve.
    requires java.net.http;
    requires com.google.gson;

    uses org.slf4j.spi.SLF4JServiceProvider;
}
