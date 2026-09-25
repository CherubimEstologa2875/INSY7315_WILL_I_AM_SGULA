// Writes quiz.json to Firestore, needs `gcloud auth application-default login` first
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const seed = require("./quiz.json");

initializeApp({ projectId: "sgula-task2-insy7315" });
const db = getFirestore();

async function main() {
  const batch = db.batch();
  for (const [categoryId, category] of Object.entries(seed.categories)) {
    batch.set(db.collection("MeditationCategory").doc(categoryId), category);
  }
  for (const [questionId, question] of Object.entries(seed.questions)) {
    batch.set(db.collection("quizQuestions").doc(questionId), { quizId: seed.quizId, ...question });
  }
  await batch.commit();
  console.log(`Seeded ${Object.keys(seed.categories).length} categories and ${Object.keys(seed.questions).length} questions`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
