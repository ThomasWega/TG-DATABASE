# TG-Toolkit
Common usage util for TrustGames.net plugins

###### Configs:
- Chat messages + colors
- Chat limit
- Commands messages
- Cooldown messages
- Cooldown values
- Permissions
- Server values
- Player data intervals
- Player data types
- RabbitMQ exchanges
- RabbitMQ queues

###### Features:
- Player Data Database + fetching
- Player Data Cache
- Player Data update event
- Player Data object
- Resolving of Player Data (first cache, then database) + updating
- Cooldown Manager
- HikariCP pool Manager
- RabbitMQ manager
- SkinData Object (texture, signature)
- SkinFetcher (from mojang-api)
- Level conversion Util
- Number conversion/verify Util
- UUID conversion/verify Util
- Color conversion Util
- Component conversion Util
- Placeholders (MiniPlaceholders)

###### How to get Toolkit:
TG-Toolkit is self-hosted on a server. To be able to reach that server you need to set the server up credentials first. 
Open (or create) gradle.properties in your local ~/.gradle/gradle.properties

**_gradle.properties_**
```
trustgamesRepoPrivateUsername={username}
trustgamesRepoPrivatePassword={secret}
```


