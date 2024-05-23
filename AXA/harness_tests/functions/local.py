def fact(n: int) -> int:
	msg = "Argument must be a non negative integer."
	try:
		if abs(int(n) -n) == 0:
			n = int(n)
			if n < 0:
				print(msg)
				raise
			if n < 2:
				return 1
			return n*fact(n -1)
	except:
		print(msg)
		raise
