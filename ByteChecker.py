file1 = open("dir1/bg.jpg", "rb")
file2 = open("dir2/bg.jpg", "rb")

a = file1.read()
b = file2.read()

count = 0
if len(a) != len(b):
    print("You failed!")
else:
    for i in range(len(a)):
        if a[i] != b[i]:
            count += 1
            print(i)
            print("Value at a[i]: %s" % str(a[i]))
            print("Value at b[i]: %s" % str(b[i]))
            break

    if count == 0:
        print("Good job!")
    else:
        print("You failed!")