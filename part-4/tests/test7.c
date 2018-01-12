
int main(int argc, char **argv) {
  int i = 666;
  int dead = 0;
  dead = i+dead;
  for (int j=0; j<3; j++) {
    dead++;
  }
  printf("%d\n",i);
}