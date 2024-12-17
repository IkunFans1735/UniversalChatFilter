# AdvancedSensitiveWords
If you trigger the sensitive word test you'll be penalised.
One-stop-shop **ultimate** anti-swear solution for your Minecraft server!

![](https://socialify.git.ci/HaHaWTH/AdvancedSensitiveWords/image?description=1&descriptionEditable=One-stop-shop%20ultimate%20anti-swear%20solution%20for%20Minecraft&font=Inter&language=1&name=1&stargazers=1&theme=Auto)

[![Available on SpigotMC](https://img.shields.io/badge/Available%20on%20SpigotMC-orange?style=for-the-badge&logo=SpigotMC&logoColor=FFFFFF)](https://www.spigotmc.org/resources/advancedsensitivewords.115484/)

[![CodeFactor](https://www.codefactor.io/repository/github/hahawth/advancedsensitivewords/badge)](https://www.codefactor.io/repository/github/hahawth/advancedsensitivewords)
[![QQ](https://img.shields.io/badge/QQ-361581545-blue)](https://qm.qq.com/q/sC52yJDrGi)

## Features
1. Using DFA(Deterministic Finite Automata) algorithm
2. Plug-and-play
3. Huge and high-quality default dictionary (Over 60,000 words)
4. Performance first, handling 1,000+ messages per second
5. 100% compatibility with chat plugins (Tested over 30+ plugins)
6. Full-customizable
7. Sign check support
8. Anvil check support
9. Book check support
10. Player name check support
11. **Chat context check**
12. **Sign multiple lines check**
13. **Book check with ignore lines support and cache**
14. Bedrock player compatibility
15. Compatibility with main stream login plugins (AuthMe, CatSeedLogin etc.)
16. Emoji and other unicode support
17. Chinese support
18. Fast processing depending on our custom data structure
19. Online sensitive word list support ([Repository here](https://github.com/HaHaWTH/ASW-OnlineWordList))
20. Folia supported
21. OP notifications on player swore
22. Custom punishments (Effect, command, hostile, etc.)
23. Fake message support (Inspired by [Bilibili Avalon System](https://github.com/freedom-introvert/Research-on-Avalon-System-in-Bilibili-Comment-Area))
24. PlaceHolder API expansion support
25. AI powered moderation system

**Features above make us unique in the anti-swear plugins!**

## How does this plugin work?

```mermaid
graph TD
    A[Player Interaction] --> B[Event Listeners]
    C[Player Packet] --> D[Packet Listeners]
    B --> E[Regex Preprocess]
    D --> E[Regex Preprocess]
    E --> F[DFA Match]
    F -->|Matched| G[Result]
    G --> J[Replace]
    G --> K[Cancel]
    F -->|Not Matched| H[AI Processor]
    H -->|LLMs| I[Rating]
    I --> L[Punish]
```

## Commands

`/asw help` - Show help message

`/asw reload` - Re-initialize the DFA dict and reload configurations

`/asw status` - Show the status of the AdvancedSensitiveWords

`/asw test <text>` - Test the AdvancedSensitiveWords filter with given text

`/asw info <player>` - Check total violations of specific player from database

`/asw punish <player> [type]` - Punish a player with a specific type

## Permissions

`advancedsensitivewords.bypass` - Bypass the AdvancedSensitiveWords message filter

`advancedsensitivewords.reload` - Allows you to use the /asw reload command

`advancedsensitivewords.status` - Allows you to use the /asw status command

`advancedsensitivewords.test` - Allows you to use the /asw test command

`advancedsensitivewords.help` - Allows you to use the /asw help command

`advancedsensitivewords.notice` - Retrieve the notification when players swore

`advancedsensitivewords.info` - Ability to use /asw info command

`advancedsensitivewords.update` - Receive update notifications

`advancedsensitivewords.punish` - Allows you to use the /asw punish command

**For more info, please visit [our Wiki](https://github.com/HaHaWTH/AdvancedSensitiveWords/wiki)**

## Supported Platforms
- Spigot(stable)
- Velocity(stable)
- BungeeCord(stable)
- Sponge(WIP)
- Fabric(Planned)
- Forge/NeoForge(Coming s∞n)

## Known Incompatibilities
- [HuskChat (Only proxy mode)](https://github.com/WiIIiam278/HuskChat)

## Statistics
[![](https://img.shields.io/bstats/servers/20661?label=Spigot%20Servers&style=for-the-badge)](https://bstats.org/plugin/bukkit/AdvancedSensitiveWords/20661)

[![](https://img.shields.io/bstats/players/20661?label=Online%20Players&style=for-the-badge)](https://bstats.org/plugin/bukkit/AdvancedSensitiveWords/20661)

## Open-source projects used
- **[Ollama4j(Modified to support Java 8)](https://github.com/ollama4j/ollama4j)**
- **[OpenAI4j](https://github.com/ai-for-java/openai4j)**
- **[packetevents(Used for handling chat & book packets)](https://github.com/retrooper/packetevents)**
