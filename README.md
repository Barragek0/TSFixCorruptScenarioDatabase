# TSFixCorruptScenarioDatabase
Attempts to diagnose and fix an issue causing train simulator to crash when updating scenario database.

The code is extremely messy, but it works fine for now, will clean it up at some point.

Steps the program takes:
[list]
[*] Finds and moves all workshop scenarios to the backup folder if found.
[*] Checks for scenario editor corruption and moves the files to the backup folder if found.
[*] Checks if the steps above fixed the issue, if they didn't: has a redundancy that moves route folders out of the routes folder 1 / 5 at a time, loading up the game in between, until coming across the faulty route, then goes through each scenario in that route until it finds the corrupt one. This step is really finicky, sometimes it works and sometimes it doesn't, the steps above should always work though.
[*] If there's still problems after the step above, it will go through all logmate files that have been created and find all the errors, outputting them to Railworks/Error-(time).txt
[/list]
