#!/usr/bin/python

import time
from datetime import datetime

class Turker(object):

	seconds_cutoff = 120

	def __init__(self, turk_id):
		self.id = turk_id
		self.time_stamps = []
		self.raw_time = []
		self.actions = []

	def append_stamp(self, stamp, action):
		# Format: 2014-07-14 01:39:00
		tm = time.strptime(stamp, "%Y-%m-%d %H:%M:%S")
		date = datetime.fromtimestamp(time.mktime(tm))
		self.time_stamps.append(date)
		self.raw_time.append(stamp)
		self.actions.append(action)

	def unix_time(self, dt):
		epoch = datetime.utcfromtimestamp(0)
		delta = dt - epoch
		return delta.total_seconds()

	def get_total_time(self):
		if len(self.time_stamps) == 0:
			return None

		discreet_blocks = []

		self.time_stamps.sort()
		prev = self.time_stamps[0]
		sdb = self.unix_time(prev)

		for date in self.time_stamps:
			last = self.unix_time(prev)
			pres = self.unix_time(date)

			if pres - last > Turker.seconds_cutoff:
				discreet_blocks.append(last - sdb)
				sdb = pres
			
			prev = date

		discreet_blocks.append(self.unix_time(prev) - sdb)

		total = 0
		for block in discreet_blocks:
			total += block

		return total

	def print_pretty(self):
		print self.id
		for i in range(0, len(self.actions)):
			print "\t" + self.raw_time[i] + " \t" + self.actions[i]
		print ""

f = open("log.txt")
lines = f.readlines()
f.close()

turkers = {}

for line in lines:
	parts = line.split('\t')

	if len(parts) < 4:
		continue

	tid = parts[3]

	worker = None
	if tid in turkers:
		worker = turkers[tid]
	else:
		worker = Turker(tid)
		turkers[tid] = worker

	worker.append_stamp(parts[0], " ".join(parts[4:]).strip())

for key in turkers:
	turker = turkers[key]
	seconds = turker.get_total_time()
	mins = int(seconds / 60)
	print key + "\t" + str(mins) + ":" + str(int(seconds % 60))

for key in turkers:
	turkers[key].print_pretty()