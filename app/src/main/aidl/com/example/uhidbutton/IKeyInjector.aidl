package com.example.uhidbutton;

interface IKeyInjector {
    void tapSpace();
    void keyDown(int hid);
    void keyUp(int hid);
    void close();
}
