# AutoWhitelist
A BungeeCord plugin which periodically syncs server whitelist with an online CSV file.

## Installation
Drop the plugin JAR file into the `/plugins` directory of your BungeeCord server, then start it.

At first run, the plugin will create the `/plugins/AutoWhitelist/config.yml` file.

## Configuration
```yaml
# URL to download the CSV file
csvURL: "https://randomurl.com/whitelist.csv"

# Refresh time, in seconds
syncTime: 60

# Servers the plugin will give join permission for
servers:
- "survival"
- "skyblock"
```
Once you edit the `config.yml` file, run the command `/aw reload`.

## Command
The main command of the plugin is `/antiwhitelist` (alias: `/aw`).

It has 4 subcommands:
- **sync**: this will instantly sync whitelist with datas fetched from the CSV file.
- **reload**: this will reload config parameters, sync the whitelist and restart the synchronization task.
- **stop**: this will shutdown the synchronization task until server restart or call to `/aw reload`.
