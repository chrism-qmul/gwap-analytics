import csv
import datetime

players = set()
duration = 0
session_space = 20 * 1000 * 60
with open("../wc.csv") as fh:
    last_game_end = 0
    for row in csv.DictReader(fh):
        if row['player'] == '289':
            start = int(row['start'])
            duration = int(row['duration'])
            game_end = start + duration
            
            print("game start",str(datetime.datetime.fromtimestamp(start/1000.0)),"game end",str(datetime.datetime.fromtimestamp(game_end/1000.0)),"game space", start - last_game_end, "game length", (((game_end-start)/1000.0)/60.0))
            if last_game_end + session_space > int(row['start']):
                print("new session")
            last_game_end = game_end
