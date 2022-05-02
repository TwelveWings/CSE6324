with open("dir1/Chapter7.ppt", "rb") as file1:
    with open("dir2/Chapter7.ppt", "rb") as file2:
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
                    #print("Value at a[i]: %s" % str(a[i]))
                    #print("Value at b[i]: %s" % str(b[i]))
                
            if count == 0:
                print("Good job!")
            else:
                print("You failed!")