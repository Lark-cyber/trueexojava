package com.devops.passwordcli;

/**
 * Contient tous les paramètres de configuration issus du parsing CLI ou du mode interactif.
 * Objet immuable passé entre les composants de l'application.
 */
public class CliConfig {

    private final int length;
    private final int count;
    private final boolean useUpper;
    private final boolean useLower;
    private final boolean useDigits;
    private final boolean useSymbols;
    private final String dockerHost;
    private final int dockerPort;
    private final boolean noDocker;

    public CliConfig(int length, int count,
                     boolean useUpper, boolean useLower,
                     boolean useDigits, boolean useSymbols,
                     String dockerHost, int dockerPort,
                     boolean noDocker) {
        this.length = length;
        this.count = count;
        this.useUpper = useUpper;
        this.useLower = useLower;
        this.useDigits = useDigits;
        this.useSymbols = useSymbols;
        this.dockerHost = dockerHost;
        this.dockerPort = dockerPort;
        this.noDocker = noDocker;
    }

    public int getLength()       { return length; }
    public int getCount()        { return count; }
    public boolean isUseUpper()  { return useUpper; }
    public boolean isUseLower()  { return useLower; }
    public boolean isUseDigits() { return useDigits; }
    public boolean isUseSymbols(){ return useSymbols; }
    public String getDockerHost(){ return dockerHost; }
    public int getDockerPort()   { return dockerPort; }
    public boolean isNoDocker()  { return noDocker; }
}
