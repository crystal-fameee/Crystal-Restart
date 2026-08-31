# Crystal-SafeRestart
Paper 1.21.11. Java 21.

Commands:
- /restart — starts a 60 second safe restart
- /restartset — saves the player's current location as the safe zone
- /restartcancel — cancels the countdown

During the countdown all players are teleported to the safe location and player damage is disabled. At 0 seconds the console executes `restart`.

Build: `mvn clean package`
Output: `target/Crystal-SafeRestart-1.0.0.jar`
