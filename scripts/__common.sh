
# tput setab [1-7] # Set the background colour using ANSI escape
# tput setaf [1-7] # Set the foreground colour using ANSI escape

# Colours are as follows:

# Num  Colour    #define         R G B

# 0    black     COLOR_BLACK     0,0,0
# 1    red       COLOR_RED       1,0,0
# 2    green     COLOR_GREEN     0,1,0
# 3    yellow    COLOR_YELLOW    1,1,0
# 4    blue      COLOR_BLUE      0,0,1
# 5    magenta   COLOR_MAGENTA   1,0,1
# 6    cyan      COLOR_CYAN      0,1,1
# 7    white     COLOR_WHITE     1,1,1
red=`tput setaf 1`
green=`tput setaf 2`
reset=`tput sgr0`

# assigns a value to a variable, for example 
# "--var1" "qwert"
# echo $var1 will print "qwert"
function processArgumentsSimpleImpl() {

    counter=1;
    for item in "$@" 
    do
        if [ $((counter%2)) -eq 0 ]
        then
            readonly "g_${varName}"=$item
        else
            varName=${item:2}
            prefix=${item:0:2}

            if [ $prefix != '--' ]; then
                echo "Invalid argument $item, please use --variable \"value\""
            fi
        fi
        counter=$((counter + 1));
    done

}
