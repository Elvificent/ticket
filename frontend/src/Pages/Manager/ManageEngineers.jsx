import React, { useState, useEffect } from 'react';
import './ManageEngineers.css';

const categoryOptions = [
  'MODEL_S', 'MODEL_3', 'MODEL_X', 'MODEL_Y', 'CYBERTRUCK'
];

const ManageEngineers = () => {
  const [engineers, setEngineers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [showAddForm, setShowAddForm] = useState(false);
  const [newEngineer, setNewEngineer] = useState({
    name: '',
    email: '',
    specialization: '',
    phone: ''
  });
  const [modalType, setModalType] = useState(null); // 'add', 'edit', 'view'
  const [selectedEngineer, setSelectedEngineer] = useState(null);
  const [editForm, setEditForm] = useState(null);
  const [editLoading, setEditLoading] = useState(false);
  const [editError, setEditError] = useState(null);
  const [viewError, setViewError] = useState(null);
  const [viewLoading, setViewLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState(null);
  const [addForm, setAddForm] = useState({
    name: '',
    email: '',
    category: '',
    level: 1,
    maxTickets: 1
  });
  const [addLoading, setAddLoading] = useState(false);
  const [addError, setAddError] = useState(null);
  const [addSuccess, setAddSuccess] = useState(null);
  const [filterCategory, setFilterCategory] = useState('');
  const [filterLevel, setFilterLevel] = useState('');
  const [filterStatus, setFilterStatus] = useState('');

  useEffect(() => {
    const fetchEngineers = async () => {
      try {
        setLoading(true);
        const response = await fetch('/api/engineers', {
          headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
        });
        if (!response.ok) throw new Error('Failed to fetch engineers');
        const data = await response.json();
        setEngineers(data);
        setLoading(false);
      } catch (err) {
        setError('Failed to fetch engineers');
        setLoading(false);
        console.error(err);
      }
    };
    fetchEngineers();
  }, []);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setNewEngineer({
      ...newEngineer,
      [name]: value
    });
  };

  const handleAddChange = (e) => {
    const { name, value } = e.target;
    setAddForm({ ...addForm, [name]: value });
  };

  const handleAddSubmit = async (e) => {
    e.preventDefault();
    setAddLoading(true);
    setAddError(null);
    setAddSuccess(null);
    try {
      const response = await fetch('/api/engineers', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({
          email: addForm.email,
          category: addForm.category,
          level: Number(addForm.level),
          maxTickets: Number(addForm.maxTickets),
          name: addForm.name
        })
      });
      if (!response.ok) throw new Error('Failed to add engineer');
      const data = await response.json();
      setEngineers([...engineers, data]);
      setAddSuccess('Engineer added successfully!');
      setTimeout(() => {
        setAddForm({ name: '', email: '', category: '', level: 1, maxTickets: 1 });
        closeModal();
      }, 1200);
    } catch (err) {
      setAddError('Failed to add engineer.');
    } finally {
      setAddLoading(false);
    }
  };

  const handleDeleteEngineer = async (id) => {
    if (window.confirm('Are you sure you want to delete this engineer?')) {
      try {
        const response = await fetch(`/api/engineers/${id}`, {
          method: 'DELETE',
          headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
        });
        if (!response.ok) throw new Error('Failed to delete engineer');
        setEngineers(engineers.filter(engineer => engineer.id !== id));
      } catch (err) {
        console.error('Failed to delete engineer:', err);
        alert('Failed to delete engineer. Please try again.');
      }
    }
  };

  // Enhanced filtering logic
  const filteredEngineers = engineers.filter(engineer => {
    // Search term match (name, email, category, level)
    const search = searchTerm.toLowerCase();
    const matchesSearch =
      (engineer.user?.name || '').toLowerCase().includes(search) ||
      (engineer.email || '').toLowerCase().includes(search) ||
      (engineer.category || '').toLowerCase().includes(search) ||
      (engineer.level + '').includes(search);

    // Category filter
    const matchesCategory = filterCategory ? engineer.category === filterCategory : true;
    // Level filter
    const matchesLevel = filterLevel ? String(engineer.level) === filterLevel : true;
    // Status filter
    const matchesStatus = filterStatus
      ? (filterStatus === 'active' ? engineer.user?.enabled : !engineer.user?.enabled)
      : true;

    return matchesSearch && matchesCategory && matchesLevel && matchesStatus;
  });

  // Modal open/close handlers
  const openAddModal = () => { setModalType('add'); setSelectedEngineer(null); };
  const openEditModal = (eng) => {
    setModalType('edit');
    setSelectedEngineer(eng);
    setEditForm({
      name: eng.user?.name || '',
      email: eng.email || '',
      category: eng.category || '',
      level: eng.level || 1,
      maxTickets: eng.maxTickets || 1
    });
    setEditError(null);
    setSuccessMsg(null);
  };
  const openViewModal = (eng) => {
    setModalType('view');
    setSelectedEngineer(eng);
    setViewError(null);
    setViewLoading(false);
  };
  const closeModal = () => {
    setModalType(null);
    setSelectedEngineer(null);
    setEditForm(null);
    setEditError(null);
    setViewError(null);
    setSuccessMsg(null);
  };

  // Edit form handlers
  const handleEditChange = (e) => {
    const { name, value } = e.target;
    setEditForm({ ...editForm, [name]: value });
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    setEditLoading(true);
    setEditError(null);
    setSuccessMsg(null);
    try {
      const response = await fetch(`/api/engineers/${selectedEngineer.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({
          email: editForm.email,
          category: editForm.category,
          level: Number(editForm.level),
          maxTickets: Number(editForm.maxTickets),
          // name is in user, so you may need to update user separately if needed
        })
      });
      if (!response.ok) throw new Error('Failed to update engineer');
      // Optionally update name in user
      // Update local state
      setEngineers(engineers.map(eng =>
        eng.id === selectedEngineer.id
          ? { ...eng, email: editForm.email, category: editForm.category, level: Number(editForm.level), maxTickets: Number(editForm.maxTickets), user: { ...eng.user, name: editForm.name } }
          : eng
      ));
      setSuccessMsg('Engineer updated successfully!');
      setTimeout(() => closeModal(), 1200);
    } catch (err) {
      setEditError('Failed to update engineer.');
    } finally {
      setEditLoading(false);
    }
  };

  if (loading) return <div className="empty-state">Loading engineers...</div>;
  if (error) return <div className="empty-state">Error: {error}</div>;

  return (
    <div className="manager-page">
      <div className="manager-header">
        <div className="manager-header-content">
          <div>
            <h1>Engineer Management</h1>
          </div>
        </div>
      </div>

      {error && (
        <div className="manager-error">
          <i className="fas fa-exclamation-circle"></i> {error}
        </div>
      )}

      {/* Modal for Add/Edit/View */}
      {modalType && (
        <div className="engineer-modal-overlay" role="dialog" aria-modal="true">
          <div className="engineer-modal">
            <button className="engineer-modal-close" aria-label="Close" onClick={closeModal}>&times;</button>
            {modalType === 'add' && (
              <div>
                <h2>Add New Engineer</h2>
                <form onSubmit={handleAddSubmit} className="engineer-edit-form">
                  <div className="engineer-form-row">
                    <label>Name</label>
                    <input type="text" name="name" value={addForm.name} onChange={handleAddChange} required />
                  </div>
                  <div className="engineer-form-row">
                    <label>Email</label>
                    <input type="email" name="email" value={addForm.email} onChange={handleAddChange} required />
                  </div>
                  <div className="engineer-form-row">
                    <label>Category</label>
                    <select name="category" value={addForm.category} onChange={handleAddChange} required>
                      <option value="">Select Category</option>
                      {categoryOptions.map(opt => <option key={opt} value={opt}>{opt}</option>)}
                    </select>
                  </div>
                  <div className="engineer-form-row">
                    <label>Level</label>
                    <input type="number" name="level" min="1" max="2" value={addForm.level} onChange={handleAddChange} required />
                  </div>
                  <div className="engineer-form-row">
                    <label>Max Tickets</label>
                    <input type="number" name="maxTickets" min="1" value={addForm.maxTickets} onChange={handleAddChange} required />
                  </div>
                  {addError && <div className="error-message" style={{ color: 'red', marginTop: 8 }}>{addError}</div>}
                  {addSuccess && <div className="success-message" style={{ color: 'green', marginTop: 8 }}>{addSuccess}</div>}
                  <div style={{ marginTop: 18, display: 'flex', gap: 10 }}>
                    <button className="action-button edit-button" type="submit" disabled={addLoading}>{addLoading ? 'Adding...' : 'Add'}</button>
                    <button className="action-button delete-button" type="button" onClick={closeModal} disabled={addLoading}>Cancel</button>
                  </div>
                </form>
              </div>
            )}
            {modalType === 'edit' && selectedEngineer && editForm && (
              <div>
                <h2>Edit Engineer</h2>
                <form onSubmit={handleEditSubmit} className="engineer-edit-form">
                  <div className="engineer-form-row">
                    <label>Name</label>
                    <input type="text" name="name" value={editForm.name} disabled />
                  </div>
                  <div className="engineer-form-row">
                    <label>Email</label>
                    <input type="email" name="email" value={editForm.email} disabled />
                  </div>
                  <div className="engineer-form-row">
                    <label>Category</label>
                    <select name="category" value={editForm.category} onChange={handleEditChange} required>
                      <option value="">Select Category</option>
                      {categoryOptions.map(opt => <option key={opt} value={opt}>{opt}</option>)}
                    </select>
                  </div>
                  <div className="engineer-form-row">
                    <label>Level</label>
                    <input type="number" name="level" min="1" max="2" value={editForm.level} onChange={handleEditChange} required />
                  </div>
                  <div className="engineer-form-row">
                    <label>Max Tickets</label>
                    <input type="number" name="maxTickets" min="1" value={editForm.maxTickets} onChange={handleEditChange} required />
                  </div>
                  {editError && <div className="error-message" style={{ color: 'red', marginTop: 8 }}>{editError}</div>}
                  {successMsg && <div className="success-message" style={{ color: 'green', marginTop: 8 }}>{successMsg}</div>}
                  <div style={{ marginTop: 18, display: 'flex', gap: 10 }}>
                    <button className="action-button edit-button" type="submit" disabled={editLoading}>{editLoading ? 'Saving...' : 'Save'}</button>
                    <button className="action-button delete-button" type="button" onClick={closeModal} disabled={editLoading}>Cancel</button>
                  </div>
                </form>
              </div>
            )}
            {modalType === 'view' && selectedEngineer && (
              <div>
                <h2>Engineer Details</h2>
                <div className="engineer-view-details">
                  <div><b>Name:</b> {selectedEngineer.user?.name}</div>
                  <div><b>Email:</b> {selectedEngineer.email}</div>
                  <div><b>Category:</b> {selectedEngineer.category}</div>
                  <div><b>Level:</b> {selectedEngineer.level}</div>
                  <div><b>Max Tickets:</b> {selectedEngineer.maxTickets}</div>
                  <div><b>Current Tickets:</b> {selectedEngineer.currentTickets}</div>
                  <div><b>Status:</b> {selectedEngineer.user?.enabled ? 'Active' : 'Inactive'}</div>
                  <div><b>User ID:</b> {selectedEngineer.user?.id}</div>
                  <div><b>Role:</b> {selectedEngineer.user?.role}</div>
                  <div><b>Created At:</b> {selectedEngineer.user?.createdAt}</div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      <div className='flex-container'>
        <div className="search-filter">
          <div className="search-input-wrapper">
            <svg className="search-icon" viewBox="0 0 24 24">
              <path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0016 9.5 6.5 6.5 0 109.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99c.41.41 1.09.41 1.5 0s.41-1.09 0-1.5l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z" />
            </svg>
            <input
              type="text"
              placeholder="Search by name, email, category, or level..."
              className="allengineers-search-input"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            {searchTerm && (
              <button
                className="delete-button search-clear-btn"
                onClick={() => setSearchTerm('')}
                aria-label="Clear search"
              >
                ×
              </button>
            )}
          </div>
          <select
            className="filter-dropdown category"
            value={filterCategory}
            onChange={e => setFilterCategory(e.target.value)}
          >
            <option value="">All Categories</option>
            {categoryOptions.map(opt => <option key={opt} value={opt}>{opt}</option>)}
          </select>
          <select
            className="filter-dropdown level"
            value={filterLevel}
            onChange={e => setFilterLevel(e.target.value)}
          >
            <option value="">All Levels</option>
            <option value="1">Level 1</option>
            <option value="2">Level 2</option>
          </select>
          <select
            className="filter-dropdown status"
            value={filterStatus}
            onChange={e => setFilterStatus(e.target.value)}
          >
            <option value="">All Status</option>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
          </select>
          <button 
            className="action-button edit-button"
            style={{ minWidth: 80 }}
            onClick={openAddModal}
          >
            Add New Engineer
          </button>
          <button
            type="button"
            className="action-button view-button"
            style={{ minWidth: 80 }}
            onClick={() => {
              setSearchTerm('');
              setFilterCategory('');
              setFilterLevel('');
              setFilterStatus('');
            }}
          >
            Reset
          </button>
        </div>

        <div className="engineer-table-wrapper">
          {filteredEngineers.length === 0 ? (
            <div className="allengineers-empty-state">No engineers found</div>
          ) : (
            <table className="manager-table engineer-table-desktop">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Category</th>
                  <th>Level</th>
                  <th>Max Tickets</th>
                  <th>Current Tickets</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredEngineers.map((engineer) => (
                  <tr key={engineer.id}>
                    <td>{engineer.user?.name || engineer.email}</td>
                    <td>{engineer.email}</td>
                    <td>{engineer.category}</td>
                    <td>{engineer.level}</td>
                    <td>{engineer.maxTickets}</td>
                    <td>{engineer.currentTickets}</td>
                    <td>
                      <span className={`status-badge status-${engineer.user?.enabled ? 'active' : 'inactive'}`}>
                        {engineer.user?.enabled ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td>
                      <button className="action-button edit-priority-button" onClick={() => openEditModal(engineer)}>Edit</button>
                      <button 
                        className="action-button delete-button"
                        onClick={() => handleDeleteEngineer(engineer.id)}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>      
      </div>
    </div>
  );
};

export default ManageEngineers;
