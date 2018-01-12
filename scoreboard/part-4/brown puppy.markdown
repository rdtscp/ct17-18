# Results at time 2018-01-12T16-23-04+0000

Results for student *brown puppy*

* * * 

## Pass llvm-pass-my-dce

Test|Correct output|Instructions count before|Expected instruction count before|Instructions count after|Expected instruction count after|Volatile instructions before DCE|Volatile instructions after DCE
:------|:-----:|------:|------:|------:|------:|------:|------:
test01|True|2|2|2|2|0|0
test02|True|3|3|2|2|0|0
test03|True|12|12|12|12|0|0
test04|True|13|13|13|13|0|0
test05|True|8|8|8|7|0|0
test06|True|10|10|10|9|0|0
test07|True|10|10|9|8|0|0
test08|True|11|11|11|11|0|0
test09|True|11|11|10|10|0|0
test10|True|19|19|19|18|0|0
test11|True|21|21|21|20|0|0
test12|True|23|23|21|20|0|0
test13|True|7|7|7|7|1|1
test14|True|17|17|17|17|2|2
test15|True|18|18|18|18|2|2
test16|True|17|17|17|17|2|2
test17|True|24|24|24|23|1|1
test18|True|32|32|32|32|3|3


* * * 

## Pass llvm-pass-simple-dce

Test|Correct output|Instructions count before|Expected instruction count before|Instructions count after|Expected instruction count after|Volatile instructions before DCE|Volatile instructions after DCE
:------|:-----:|------:|------:|------:|------:|------:|------:
test01|True|2|2|2|2|0|0
test02|True|3|3|2|2|0|0
test03|True|12|12|12|12|0|0
test04|True|13|13|13|13|0|0
test05|True|8|8|8|8|0|0
test06|True|10|10|10|10|0|0
test07|True|10|10|9|9|0|0
test08|True|11|11|11|11|0|0
test09|True|11|11|10|10|0|0
test10|True|19|19|19|19|0|0
test11|True|21|21|21|21|0|0
test12|True|23|23|21|21|0|0
test13|True|7|7|7|7|1|1
test14|True|17|17|17|17|2|2
test15|True|18|18|18|18|2|2
test16|True|17|17|17|17|2|2
test17|True|24|24|24|24|1|1
test18|True|32|32|32|32|3|3


