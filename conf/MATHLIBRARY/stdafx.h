#pragma once
#include "targetver.h"

#define WIN32_LEAN_AND_MEAN             // Исключите редко используемые компоненты из заголовков Windows
#include <windows.h>

//OLD  #define _DLLAPI extern "C" __declspec(dllexport)
//WELL    #define _DLLAPI __declspec(dllexport)
// ???  #define _DLLAPI  extern "C++" __declspec(dllexport)
#define _DLLAPI extern "C" __declspec(dllexport)
