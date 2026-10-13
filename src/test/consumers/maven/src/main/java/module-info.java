/** Resolves the library through its Automatic-Module-Name on the module path. */
module teamspeak3.consumer {
    requires com.github.theholywaffle.teamspeak3;
    requires org.slf4j;
    requires java.logging;

    uses org.slf4j.spi.SLF4JServiceProvider;
}
