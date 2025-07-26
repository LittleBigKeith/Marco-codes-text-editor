#include<termios.h>
#include<stdio.h>
#include<stdlib.h>
#include<sys/ioctl.h>
#include<errno.h>

int main(int argc, char *argv[]) {
    printf("ISIG=%d\n", ISIG);
    printf("ICANON=%d\n", ICANON);
    printf("ECHO=%d\n", ECHO);
    printf("TCSAFLUSH=%d\n", TCSAFLUSH);
    printf("IXON=%d\n", IXON);
    printf("ICRNL=%d\n", ICRNL);
    printf("IEXTEN=%d\n", IEXTEN);
    printf("OPOST=%d\n", OPOST);
    printf("VMIN=%d\n", VMIN);
    printf("VTIME=%d\n", VTIME);
    printf("TIOCGWINSZ=%lu\n", TIOCGWINSZ);
    printf("IOC_IN=%u\n", IOC_IN);
    printf("IOC_OUT=%u\n", IOC_OUT);
    printf("IOC_INOUT=%u\n", IOC_INOUT);

    struct winsize winsize;

    printf("%d\n", ioctl(0, TIOCGWINSZ, &winsize));
    printf("%d\n", winsize.ws_row);
    printf("%d\n", winsize.ws_col);
    printf("%d\n", ioctl(1, TIOCGWINSZ, &winsize));
    printf("%d\n", winsize.ws_row);
    printf("%d\n", winsize.ws_col);
    printf("sizeof winsize: %lu", sizeof(struct winsize));

}
