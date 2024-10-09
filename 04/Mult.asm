// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/4/Mult.asm

// Multiplies R0 and R1 and stores the result in R2.
// (R0, R1, R2 refer to RAM[0], RAM[1], and RAM[2], respectively.)
// The algorithm is based on repetitive addition.

    // Initialize R2 to 0
    @R2
    M=0

    // n (index) will be counted up from 0 to R1
    @n
    M=1

(LOOP)
    // if (n > R1) goto END
    @n
    D=M
    @R1
    D=D-M
    @END
    D;JGT

    // Add R0 to current value stored at R2
    @R0
    D=M
    @R2
    M=D+M

    // n = n + 1
    @n
    M=M+1

    // goto LOOP
    @LOOP
    0;JMP

(END)
    @END
    0;JMP

