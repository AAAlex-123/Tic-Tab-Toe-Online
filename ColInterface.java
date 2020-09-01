interface ColInterface {
	public int getCode();
}
/*
eeded for Generics in Collection.
Class Collection calls method getCode() on object ob which is, by default, type Object.
By making this Interface and having Action, Data and Employee implement it,
we make sure that when we define <T extends ColInterface> in the generic definition of class Collection,
all ojbects T "know" what getCode() is, as it's defined in the Interface ColInterface.
*/