#include <cstdio>
#include <fstream>
#include <iostream>
#include <ctime>
#include <cmath>
#include <cstdlib>
#include <papi.h>
#include <sstream>
#include "papi_macro.hpp"

using namespace std;

#define SYSTEMTIME clock_t

#define NUMBER_OF_TRIES 3
#define BLOCK_SIZE 512

void printResultMatrix(double* phc, size_t& matrixSize, ostream& debugStream = cerr) {
  for (size_t i = 0; i < 1; i++)
  {
    for (size_t j = 0; j < min((size_t) 10, matrixSize); j++)
      debugStream << phc[j] << ' ';
  }
  debugStream << ';';
}

int getBlockSize() {
  int blockSize;
  
  cout << "Block Size? ";
  cin >> blockSize;

  return blockSize;
}

void resetMatrix(double* pha, double* phb, double* phc, const int& matrixSize) {
  for (size_t i = 0; i < matrixSize; i++) {
    for (size_t j = 0; j < matrixSize; j++) {
      pha[i * matrixSize + j] = (double)1.0;
      phb[i * matrixSize + j] = (double)(i + 1);
      phc[i * matrixSize + j] = (double)0.0;
    }
  }
}

void allocateAndInitMatrix(double** pha, double** phb, double** phc, const int& matrixSize) {
  *pha = (double *)malloc((matrixSize * matrixSize) * sizeof(double));
  *phb = (double *)malloc((matrixSize * matrixSize) * sizeof(double));
  *phc = (double *)malloc((matrixSize * matrixSize) * sizeof(double));

  resetMatrix(*pha, *phb, *phc, matrixSize);
}

void freeAllMatrix(double* pha, double* phb, double* phc) {
  free(pha);
  free(phb);
  free(phc);
}

void OnMult(double* pha, double* phb, double* phc, size_t matrixSize, size_t blockSize = 0, double *timeRes = NULL)
{
  SYSTEMTIME Time1, Time2;

  char st[100];
  int i, j, k;

  Time1 = clock();

  for (i = 0; i < matrixSize; i++)
  {
    for (j = 0; j < matrixSize; j++)
    {
      for (k = 0; k < matrixSize; k++)
      {
        phc[i * matrixSize + j] += pha[i * matrixSize + k] * phb[k * matrixSize + j];
      }
    }
  }

  Time2 = clock();

  if (timeRes != NULL)
  {
    *timeRes = (double)(Time2 - Time1) / CLOCKS_PER_SEC;
  }
}

// add code here for line x line matriz multiplication
void OnMultLine(double* pha, double* phb, double* phc, size_t matrixSize, size_t blockSize = 0, double *timeRes = NULL)
{
  SYSTEMTIME Time1, Time2;

  char st[100];
  int i, j, k;

  Time1 = clock();

  for (i = 0; i < matrixSize; i++)
  {
    for (j = 0; j < matrixSize; j++)
    {
      for (k = 0; k < matrixSize; k++)
      {
        phc[i * matrixSize + k] += pha[i * matrixSize + j] * phb[j * matrixSize + k];
      }
    }
  }

  Time2 = clock();

  if (timeRes != NULL)
  {
    *timeRes = (double)(Time2 - Time1) / CLOCKS_PER_SEC;
  }
}

// add code here for block x block matriz multiplication
void OnMultBlock(double* pha, double* phb, double* phc, size_t matrixSize, size_t blockSize, double *timeRes = NULL)
{
  cout << "Running with block size " << blockSize << endl;

  if (matrixSize % blockSize != 0)
  {
    return;
  }

  SYSTEMTIME Time1, Time2;

  char st[100];
  int i0, j0, k0, i, j, k, numberOfBlocks;

  numberOfBlocks = (matrixSize / blockSize);

  Time1 = clock();

  for (i0 = 0; i0 < matrixSize; i0 += blockSize)
  {
    for (j0 = 0; j0 < matrixSize; j0 += blockSize)
    {
      for (k0 = 0; k0 < matrixSize; k0 += blockSize)
      {
        for (i = i0; i < i0 + blockSize; i++)
        {
          for (j = j0; j < j0 + blockSize; j++)
          {
            for (k = k0; k < k0 + blockSize; k++)
            {
              phc[i * matrixSize + k] += pha[i * matrixSize + j] * phb[j * matrixSize + k];
            }
          }
        }
      }
    }
  }

  Time2 = clock();

  if (timeRes != NULL)
  {
    *timeRes = (double)(Time2 - Time1) / CLOCKS_PER_SEC;
  }
}

void handle_error(int retval)
{
  printf("PAPI error %d: %s\n", retval, PAPI_strerror(retval));
  exit(1);
}

void init_papi(int *EventSet) {
    int retval = PAPI_library_init(PAPI_VER_CURRENT);
    if (retval != PAPI_VER_CURRENT && retval < 0) {
        printf("PAPI library version mismatch!\n");
        exit(1);
    }
    if (retval < 0)
        handle_error(retval);

    std::cout << "PAPI Version Number: MAJOR: " << PAPI_VERSION_MAJOR(retval)
              << " MINOR: " << PAPI_VERSION_MINOR(retval)
              << " REVISION: " << PAPI_VERSION_REVISION(retval) << "\n";

    retval = PAPI_create_eventset(EventSet);
    if (retval != PAPI_OK)
        cout << "ERROR: create eventset" << endl;

    //TODO: Estudar efeitos dos eventos PAPI aqui


    retval = PAPI_add_event(*EventSet, PAPI_L1_DCM);
    if (retval != PAPI_OK)
        cout << "ERROR: PAPI_L1_DCM" << endl;

    retval = PAPI_add_event(*EventSet, PAPI_L2_DCM);
    if (retval != PAPI_OK)
        cout << "ERROR: PAPI_L2_DCM" << endl;

    retval = PAPI_add_event(*EventSet, PAPI_L2_DCA);
    if (retval != PAPI_OK)
        cout << "ERROR: PAPI_L2_DCA" << endl;

    retval = PAPI_add_event(*EventSet, PAPI_L3_DCA);
    if (retval != PAPI_OK)
        cout << "ERROR: PAPI_L3_DCA" << endl;
}

void destroy_papi(int EventSet) {
  int ret;
  ret = PAPI_remove_event(EventSet, PAPI_L1_DCM);
  if (ret != PAPI_OK)
    std::cout << "FAIL remove event" << endl;

  ret = PAPI_remove_event(EventSet, PAPI_L2_DCM);
  if (ret != PAPI_OK)
    std::cout << "FAIL remove event" << endl;

  ret = PAPI_destroy_eventset(&EventSet);
  if (ret != PAPI_OK)
    std::cout << "FAIL destroy" << endl;
}

void benchmark(string filePrefix, size_t initialSize, size_t finalSize, size_t step, size_t blockSize, int EventSet, void (*action)(double*, double*, double*, size_t, size_t, double*))
{
  double* pha;
  double* phb;
  double* phc;

  long long eventValues[NUMBER_OF_PAPI_EVENTS];
  long long executionEventValues[NUMBER_OF_PAPI_EVENTS];
  char filename[256];
  for (size_t matrixSize = initialSize; matrixSize <= finalSize; matrixSize += step)
  {
    allocateAndInitMatrix(&pha, &phb, &phc, matrixSize);
    sprintf(filename, "%s_result_%ld.csv", filePrefix.c_str(), matrixSize);
    stringstream ss;
    ss << "../doc/execution_data/" << filePrefix << "/" << filename;
    ofstream benchmarkFile(ss.str());
    benchmarkFile << "Iteration;Result Matrix;";
    for (size_t i = 0; i < NUMBER_OF_PAPI_EVENTS; i++ ) {
      benchmarkFile << "Event " << i << ';';
    }
    benchmarkFile << "Execution Time" << endl;
    double avgExecutionTime = 0;
    for(size_t i = 0; i < NUMBER_OF_PAPI_EVENTS; i++)
    {
      eventValues[i] = 0;
    }
    for (size_t exe = 0; exe < NUMBER_OF_TRIES; exe++) {
      benchmarkFile << exe << ';';
      double executionTime = 0;
      start_papi_event_counter(EventSet);
      action(pha, phb, phc, matrixSize, blockSize, &executionTime);
      stop_papi_event_counter(EventSet, executionEventValues);
      printResultMatrix(phc, matrixSize, benchmarkFile);
      for (size_t i = 0; i < NUMBER_OF_PAPI_EVENTS; i++) {
        benchmarkFile << executionEventValues[i] << ';';
        eventValues[i] += executionEventValues[i];
      }
      benchmarkFile << executionTime << std::endl;
      avgExecutionTime += executionTime;
      resetMatrix(pha, phb, phc, matrixSize);
      reset_papi_event_counter(EventSet);
    }
    freeAllMatrix(pha, phb, phc);
    avgExecutionTime /= NUMBER_OF_TRIES;
    for (int i = 0; i < NUMBER_OF_PAPI_EVENTS; i++) {
      eventValues[i] = (long long) round(eventValues[i] / (double) NUMBER_OF_TRIES);
    }
    benchmarkFile << "Average;;";
    for (size_t i = 0; i < NUMBER_OF_PAPI_EVENTS; i++) {
      benchmarkFile << eventValues[i] << ";";
    }
    benchmarkFile << avgExecutionTime << std::endl;
    benchmarkFile.close();
  }
}

int main(int argc, char *argv[])
{
  double* pha;
  double* phb;
  double* phc;

  pha = phb = phc =  NULL;

  char c;
  size_t lin, col, blockSize;
  int op;

  int EventSet = PAPI_NULL;
  long long values[NUMBER_OF_PAPI_EVENTS];
  
  init_papi(&EventSet);

  op = 1;
  do
  {
    cout << endl
         << "1. Multiplication" << endl;
    cout << "2. Line Multiplication" << endl;
    cout << "3. Block Multiplication" << endl;
    cout << "4. Benchmark Multiplication(600 to 3000 step 400)" << endl;
    cout << "5. Benchmark Line Multiplication(600 to 3000 step 400)" << endl;
    cout << "6. Benchmark Line Multiplication(4096 to 10240 step 2048)" << endl;
    cout << "7. Benchmark Block Multiplication(4096 to 10240 step 2048)" << endl;
    cout << "Selection?: ";
    cin >> op;
    if (op == 0)
      break;
    if (op < 4) {
      printf("Dimensions: lins=cols ? ");
      cin >> lin;
      col = lin;
    }

    size_t blockSize;

    if (op < 4) {
      allocateAndInitMatrix(&pha, &phb, &phc, lin);
    }
    double timeRes;
    switch (op)
    {
      case 1:
        OnMult(pha, phb, phc, lin, 0, &timeRes);
        std::cout << "Execution time: " << timeRes << "seconds" << std::endl;
        break;
      case 2:
        OnMultLine(pha, phb, phc, lin, 0, &timeRes);
        std::cout << "Execution time: " << timeRes << "seconds" << std::endl;
        break;
      case 3:
        blockSize = getBlockSize();
        if (blockSize != -1) {
          OnMultBlock(pha, phb, phc, lin, blockSize, &timeRes);
        } else {
          cout << "Invalid block size!" << endl;
        }
        std::cout << "Execution time: " << timeRes << "seconds" << std::endl;
        break;
      case 4:
        benchmark("mult", 600, 3000, 400, blockSize, EventSet, OnMult);
        break;
      case 5:
        benchmark("mult_line", 600, 3000, 400, blockSize, EventSet, OnMultLine);
        break;
      case 6:
        benchmark("mult_line", 4096, 10240, 2048, blockSize, EventSet, OnMultLine);
        break;
      case 7: {
        blockSize = getBlockSize();
        if (blockSize != -1) {
          char multBlockFilePrefix[50];
          sprintf(multBlockFilePrefix, "mult_block_%d", blockSize);
          benchmark(multBlockFilePrefix, 4096, 10240, 2048, blockSize, EventSet, OnMultBlock);
        } else {
          cout << "Invalid block size!" << endl;
        }
        break;
      }
    }

    if (op < 4) {
      freeAllMatrix(pha, phb, phc);
    }

  } while (op != 0);

  destroy_papi(EventSet);
}