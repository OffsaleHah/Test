package com.example.uhidbutton;

interface IKeyInjector {
    void tapSpace();
    void keyDown(int keyCode);
    void keyUp(int keyCode);
    void close();
}
