import React, { useState } from "react";
import "./FAQ.css";

const faqs = [
  {
    question: "What is the AI assistant and how does it help customers?",
    answer: "The AI assistant uses Retrieval-Augmented Generation (RAG) to instantly answer common queries based on your knowledge base, reducing wait times.",
  },
  {
    question: "How does the system escalate unresolved queries to human engineers?",
    answer: "If the AI assistant cannot resolve a query, it automatically creates a ticket and assigns it to the appropriate engineer based on skills and availability.",
  },
  {
    question: "Can customers track their ticket status in real time?",
    answer: "Yes, customers can log in to their portal to view ticket updates, engineer responses, and resolution progress in real-time.",
  },
  {
    question: "What’s the difference between Level 2 and Level 3 engineers?",
    answer: "In our system, the AI Support Assistant replaces traditional Level 1 engineers by handling general inquiries and basic troubleshooting. Level 2 engineers (Product-generalists) handle more complex product problems that the AI cannot resolve. If needed, they escalate issues to Level 3 engineers (Product-specialists), who focus on specialized, high-impact issues and coordinate with the development team for resolution.",
  },
  {
    question: "How does the system ensure the right engineer is assigned to a task?",
    answer: "Engineer profiles track skills and workloads. The system uses this data to automatically assign the most suitable engineer to each task.",
  },
  {
    question: "Is our customer data secure in this system?",
    answer: "Yes, all sensitive information is encrypted and access is role-based. The system follows best practices for GDPR-compliant data protection.",
  },
  {
    question: "Can managers monitor team performance with dashboards?",
    answer: "Absolutely. Managers have access to dashboards that display key performance indicators like resolution time, ticket volume, and escalation trends.",
  },
  {
    question: "Is the system suitable for small to medium-sized businesses?",
    answer: "Yes, it’s designed to scale from startups to enterprises, with customizable modules and flexible pricing options.",
  },
  {
    question: "How does the AI assistant handle knowledge base updates?",
    answer: "The assistant pulls real-time updates from the knowledge base, so any edits or additions are instantly reflected in customer responses.",
  },
  {
    question: "Can the platform integrate with existing CRM tools?",
    answer: "Yes, it provides API endpoints to sync with popular CRM tools for unified customer records and streamlined workflows.",
  },
];

const FAQ = () => {
  const [openIndex, setOpenIndex] = useState(null);

  const toggleFAQ = (index) => {
    setOpenIndex(openIndex === index ? null : index);
  };

  return (
    <div className="faq-container">
      <h2 className="faq-title">Frequently asked questions, answered.</h2>
      <div className="faq-list">
        {faqs.map((faq, index) => (
          <div key={index} className="faq-item">
            <div
              className="faq-question"
              onClick={() => toggleFAQ(index)}
            >
              {faq.question}
              <span className="faq-toggle">{openIndex === index ? "−" : "+"}</span>
            </div>
            {openIndex === index && (
              <div className="faq-answer">{faq.answer}</div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default FAQ;
