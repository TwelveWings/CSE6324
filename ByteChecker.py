file1 = open("bg.jpg", "rb")
file2 = open("bg3.jpg", "rb")

a = file1.read()
b = file2.read()

count = 0
for i in range(len(a)):
    print(i)
    if a[i] != b[i]:
        count += 1
        print(i)
        print("Value at a[i]: %s" % str(a[i]))
        print("Value at b[i]: %s" % str(b[i]))
        
if count == 0:
    print("Good job!")
else:
    print("You failed!")